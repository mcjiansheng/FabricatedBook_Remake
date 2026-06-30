# 后端藏品实现状态审计

> 更新时间：2026-06-30  
> 范围：`core/src/main/resources/data/relics.json`、`doc/game_encyclopedia/relics.md`、`DataRelic`、`RelicManager`、`NodeEntryResolver`、`EnemyEncounterResolver`、`EventRewardResolver`。

本文用于承接根目录 `TODO.md` 中“梳理藏品实现状态”的任务。结论按当前后端代码判断，不代表所有玩法数值已经最终定稿。

## 总体结论

当前 `relics.json` 共 49 个藏品或奖励占位 ID：

- 已完整接入：46 个。
- 部分实现：2 个。
- 需要规则确认：1 个。
- 未找到完全缺失实现的普通藏品。

后端藏品入口分布如下：

- `DataRelic`：即时获得、伤害修正、治疗修正、状态伤害、回合开始、战斗开始订阅、敌人开战生命修正。
- `RelicManager`：统一调度、商店进入、战斗胜利、战斗奖励概率。
- `NodeEntryResolver`：进入节点时的 `"集权"` / `"寡头"`。
- `EnemyEncounterResolver`：`"复仇者"` 第 3 层强制 Boss、`"巴别塔"` 紧急作战额外敌人和第 5 层隐藏 Boss 路线。
- `EventRewardResolver`：事件奖励占位 ID 展开，包括五张牌、随机低阶藏品、随机负面藏品，以及显式未接入的核弹。

## 需要优先处理的问题

### R-001：`relic_frostmourne` 累计胜利数没有进入存档

- 状态：部分实现。
- 位置：`DataRelic.onCombatVictory()`、`DataRelic.modifyOutgoingDamage()`、`SaveManager`、`GameRunState.PlayerSnapshot`。
- 当前行为：战斗胜利会增长 `DataRelic.combatWins`，本次运行内伤害加成可生效。
- 问题原因：`combatWins` 是 `DataRelic` 实例字段；保存/读档只保存 `relicIds`，读档后通过 `RelicFactory.createById()` 重建藏品实例，累计胜利数回到 0。
- 期望实现：把霜之哀伤这种跨战斗成长状态归属到对局/玩家快照中，例如 `Player.frostmourneCombatWins` 或更通用的 per-relic state；`SaveManager` 保存恢复该字段，`DataRelic` 从稳定状态读取。
- 影响范围：存档格式、`GameRunState.PlayerSnapshot`、`SaveManager`、`DataRelicTest` 或存档测试。需要保持旧存档缺字段时默认 0。

### R-002：`relic_bankbook` 触发点绑定在商店商品生成

- 状态：部分实现。
- 位置：`RelicManager.onEnterShop()`、`ShopManager.generateItems()`。
- 当前行为：每次调用 `generateItems()` 都会触发“捡来的存折”获得 25 金币。
- 问题原因：效果语义是“每次进入商店获得 25 块”，但当前触发点是商品列表生成。正常前端/CLI 流程通常只生成一次商品，因此主链路可用；如果未来出现刷新商品、重建商店界面、读档恢复商店过程，可能重复触发。
- 期望实现：将商店进入副作用收口到节点进入或商店会话初始化中，并用 `GameRunState` 的 active node 提交语义保证每个商店节点只触发一次。
- 影响范围：`ShopManager`、`NodeEntryResolver`、`GameRunState` 非战斗节点提交、商店读档流程。

### R-003：`relic_nuke` 真实玩法语义未定义

- 状态：需要规则确认。
- 位置：`EventRewardResolver`、`events.json` 曼哈顿计划事件。
- 当前行为：`relic_nuke` 被识别为特殊奖励 ID，但 executor 返回显式未接入状态，不会作为普通藏品加入背包。
- 问题原因：百科和数据只说明它是村庄事件获得的特殊物品，未定义它是藏品、一次性道具、路线标记、战斗效果还是结局条件。
- 期望实现：先确认玩法语义，再实现专用 executor；确认前继续保持显式未接入，避免伪装成普通藏品。
- 影响范围：事件奖励、前端事件结果展示、存档、可能的结局路线或战斗效果。

## 逐项状态表

| ID | 名称 | 状态 | 后端入口 | 备注 |
|:--|:--|:--|:--|:--|
| `relic_hot_water_flask` | 热水壶 | 已完整 | `DataRelic.applyOnPickup()` | 最大生命 +5 并同步治疗。 |
| `relic_old_coin` | 古旧钱币 | 已完整 | `DataRelic.applyOnPickup()` | 立即获得 30 金币。 |
| `relic_weird_flute` | 古怪的长笛 | 已完整 | `DataRelic.subscribe()` / `OnCombatStart` | 战斗开始获得 1 能量。 |
| `relic_speech_draft` | 一份演讲稿 | 已完整 | `DataRelic.applyOnPickup()` | 从当前职业可获得卡池添加 1 张牌。 |
| `relic_lucky_coin` | 幸运硬币 | 已完整 | `DataRelic.applyOnPickup()` | 获得 1 金币；“幸运一点”按数据描述无实际作用。 |
| `relic_a_combo` | A 级连招 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 稳定伤害 +5%，预览同步。 |
| `relic_target` | 靶子 | 已完整 | `DataRelic.modifyOutgoingDamage()` | 33% 概率 +10%；预览不掷随机。 |
| `relic_vampire_count` | 探灵伯爵 | 已完整 | `DataRelic.modifyRelicRewardChance()` / `BattleScreen` | 战斗后藏品奖励概率 +15；数值硬编码在代码中。 |
| `relic_best_of_year` | 年度最佳 | 已完整 | 无运行效果 | 数据明确为无实际作用。 |
| `relic_flawless_jade` | 无暇宝玉 | 已完整 | `DataRelic.applyOnPickup()` | 立即获得 100 金币。 |
| `relic_treasure_ring` | 至宝指环 | 已完整 | `DataRelic.modifyGoldReward()` | 战斗金币奖励 +50%。 |
| `relic_gargoyle_statue` | 石像鬼塑像 | 已完整 | `DataRelic.applyOnPickup()` | 最大生命 +10 并同步治疗。 |
| `relic_rice_bowl` | 饭碗 | 已完整 | `DataRelic.applyOnPickup()` | 按最大生命回复 25%。 |
| `relic_bankbook` | 捡来的存折 | 部分实现 | `RelicManager.onEnterShop()` / `ShopManager.generateItems()` | 主流程可用，但触发点应从商品生成迁到商店节点进入。 |
| `relic_vampire_fang` | 吸血鬼的尖牙 | 已完整 | `DataRelic.subscribe()` / `OnCardUsed` | 每使用 1 张牌回复 1 点生命。 |
| `relic_kungfu_manual` | 葵花宝典 | 已完整 | `DataRelic.modifyOutgoingDamage()` | 33% 概率 +80%；预览不掷随机。 |
| `relic_miniature_stage` | 微缩舞台模型 | 已完整 | `DataRelic.subscribe()` / `OnCombatStart` | 战斗开始获得 3 能量并抽 2 张牌。 |
| `relic_prop_box` | 道具箱 | 已完整 | `DataRelic.applyOnPickup()` | 从当前职业可获得卡池添加 3 张牌。 |
| `relic_coin_toy` | 投币玩具 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 每 10 金币伤害 +3%，预览同步。 |
| `relic_hand_soap` | 洗手液 | 已完整 | `DataRelic.modifyHeal()` | 治疗 +20%。 |
| `relic_marionette` | 悬丝木偶 | 已完整 | `DataRelic.modifyStatusDamage()` | 中毒和凋零伤害 +10%。 |
| `relic_5a_combo` | 5A 级连招 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 稳定伤害 +25%，预览同步。 |
| `relic_king_spear` | 国王的长枪 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 生命不高于 10% 时伤害 +50%。 |
| `relic_king_armor` | 国王的铠甲 | 已完整 | `DataRelic.onTurnStart()` | 生命不高于 10% 时施加 `BlockIncrease` 并获得 10 格挡。 |
| `relic_king_crystal` | 国王的水晶 | 已完整 | `DataRelic.modifyGoldReward()` | 战斗金币奖励结算时，高于 10% 生命扣 5 点并 +20 金币。 |
| `relic_blade_light` | 刀光剑影 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 按玩家负面 Buff 种类计数加伤，预览同步。 |
| `relic_frostmourne` | 霜之哀伤 | 部分实现 | `DataRelic.onCombatVictory()` / `modifyOutgoingDamage()` | 运行内可成长，但累计胜利数未保存恢复。 |
| `relic_peeping_eye` | 窥秘之眼 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 敌方受到伤害 +40%，预览同步。 |
| `relic_i_help_you` | "我来助你" | 已完整 | `DataRelic.onTurnStart()` | 回合开始对所有存活敌人造成 2 点伤害。 |
| `relic_safety_suit` | 蓝卡坞安全衣 | 已完整 | `DataRelic.modifyIncomingDamage()` | 每个负面藏品使受到伤害 -10%。 |
| `relic_golden_cup` | 金酒之杯 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 每 10 金币伤害 +8%，预览同步。 |
| `relic_old_fan` | 老蒲扇 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `previewOutgoingDamage()` | 按牌组/手牌/弃牌/消耗牌总数加伤，预览同步。 |
| `relic_rosemary_squad` | 迷迭香小队 | 已完整 | `DataRelic.modifyOutgoingDamage()` / `modifyIncomingDamage()` | 玩家伤害 +30%，受到伤害 -30%。 |
| `relic_pale_crown` | 苍白花冠 | 已完整 | `DataRelic.modifyHeal()` | 治疗 +35%。 |
| `relic_wither_storm` | "凋零风暴" | 已完整 | `DataRelic.modifyStatusDamage()` / `onTurnStart()` | 凋零伤害 +50%，回合开始额外触发凋零。 |
| `relic_stop_war` | 止戈 | 已完整 | `DataRelic.modifyIncomingDamage()` | 受到伤害变为 70%。 |
| `relic_betrayal` | 背叛 | 已完整 | `DataRelic` / `EnemyEncounterResolver` | 1-4 层加伤、第 5 层敌人加血、隐藏路线条件均接入。 |
| `relic_hatred` | 仇恨 | 已完整 | `DataRelic` / `EnemyEncounterResolver` | 1-4 层减伤、第 5 层敌人减血、隐藏路线条件均接入。 |
| `relic_avenger` | "复仇者" | 已完整 | `DataRelic.modifyOutgoingDamage()` / `EnemyEncounterResolver` | 1/3 概率 +30%，第 3 层强制「迷失的守林人」。 |
| `relic_nuke` | 核弹 | 需要规则确认 | `EventRewardResolver` | 显式未接入特殊奖励；确认语义前不加入背包。 |
| `relic_five_cards` | 五张牌 | 已完整 | `EventRewardResolver` | 事件奖励占位，展开为 5 张真实卡牌，不作为藏品持有。 |
| `relic_random_leq3` | 随机低阶藏品 | 已完整 | `EventRewardResolver` | 事件奖励占位，展开为价值 3 及以下的非负面、非特殊藏品。 |
| `relic_curse_random` | 随机负面藏品 | 已完整 | `EventRewardResolver` | 事件奖励占位，展开为真实负面藏品。 |
| `relic_babel_tower` | "巴别塔" | 已完整 | `EnemyEncounterResolver` / `EventHandler` | 门扉事件授予；紧急作战额外敌人；与背叛/仇恨开启隐藏 Boss 路线。 |
| `relic_centralization` | "集权" | 已完整 | `NodeEntryResolver` / `DataRelic` / `SaveManager` | 战斗节点进入次数累计，伤害和预览读取，存档保存恢复。 |
| `relic_oligarch` | "寡头" | 已完整 | `NodeEntryResolver` | 非战斗节点进入获得 20 金币。 |
| `relic_sea_metabolism` | 海神的代谢 | 已完整 | `DataRelic.modifyEnemyAtCombatStart()` | 开战时敌人生命 +25%。 |
| `relic_tolerance` | 宽容 | 已完整 | `DataRelic.modifyIncomingDamage()` | 敌方造成伤害 +20%。 |
| `relic_humility` | 谦虚 | 已完整 | `DataRelic.applyOnPickup()` | 最大生命降低 30%，当前生命夹到新上限。 |

## 后续建议

1. 先修 `relic_frostmourne` 的存档状态，这是已确认的后端结构问题。
2. 再修 `relic_bankbook` 的触发归属，把“进入商店”从商品生成中剥离出来。
3. `relic_nuke` 不继续实现，直到玩法语义明确。
4. 如果继续扩展藏品，优先考虑把藏品效果从 `DataRelic` 的多个 switch 收敛为注册表，至少给“随机类效果不参与预览”建立统一约定。
