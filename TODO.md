# FabricatedBookRemake — 开发任务清单

> Java + LibGDX 重构杀戮尖塔  
> 最后更新：2026-06-12

---

## 阶段一：项目初始化 ✓

- [x] 配置 Gradle 构建文件（`build.gradle` + `settings.gradle`）
- [x] 配置 LibGDX 依赖
- [x] 配置 `desktop/src/main/java/.../DesktopLauncher.java`

## 阶段二：核心接口层 ✓

- [x] `CombatAction.java` — 战斗动作接口
- [x] `BuffHook.java` — Buff 生命周期钩子
- [x] `GameEvent.java` — 游戏事件基类

## 阶段三：实体系统 ✓

- [x] `AbstractEntity.java` — 实体基类（HP/Block/Buff）
- [x] `Player.java` — 玩家实体
- [x] `Enemy.java` — 怪物实体
- [x] `EntityFactory.java` — 实体工厂

## 阶段四：Action 系统 ✓

- [x] `DamageAction.java` — 伤害动作
- [x] `HealAction.java` — 回血动作
- [x] `ApplyBuffAction.java` — 施加 Buff
- [x] `GainEnergyAction.java` — 获得能量
- [x] `DrawCardAction.java` — 抽牌动作
- [x] `GainBlockAction.java` — 获得格挡
- [x] `RemoveCardAction.java` — 移除卡牌

## 阶段五：Buff 系统 ✓

- [x] `Fragile.java` — 脆弱（受伤害 +25%）
- [x] `BlockReduction.java` — 易碎（格挡 -50%）
- [x] `Resistance.java` — 抗性（受伤害 -25%）
- [x] `BlockIncrease.java` — 坚强（格挡 +50%）
- [x] `Weak.java` — 虚弱（伤害 -25%）
- [x] `Strength.java` — 力量（伤害 +25%）
- [x] `Dizziness.java` — 眩晕（无法行动）
- [x] `Poison.java` — 中毒（持续伤害）
- [x] `Withering.java` — 凋零（可引爆）
- [x] `ArmorBuff.java` — 装甲（格挡不消失）
- [x] `UndeadBuff.java` — 不死
- [x] `ExtraEnergyBuff.java` — 额外能量
- [x] `AbstractBuff.java` — Buff 骨架基类

## 阶段六：卡牌系统 ✓

- [x] `Card.java` — 卡牌数据载体（含枚举：CardType、TargetType）
- [x] `CardPool.java` — 卡牌池（战士卡牌）
- [x] `CardFactory.java` — 从 JSON 生成卡牌

## 阶段七：藏品系统 ✓

- [x] `Relic.java` — 藏品接口
- [x] `EventBus.java` — 事件总线
- [x] `RelicManager.java` — 藏品管理器
- [x] `RelicData.java` / `DataRelic.java` / `RelicFactory.java` — JSON 藏品实例化
- [x] 商店、事件、战斗奖励可获得真实藏品
- [x] 常用即时/被动效果已接入
- [ ] 少数高度特殊藏品仍需专门实现完整原版规则

## 阶段八：地图系统 ✓

- [x] `MapConfig.java` — 地图配置
- [x] `Node.java` — 地图节点
- [x] `NodeFactory.java` — 节点工厂（权重随机）
- [x] `MapGraph.java` — 图结构

## 阶段九：战斗引擎 ✓

- [x] `CombatEngine.java` — 战斗引擎
- [x] `ActionManager.java` — Action 队列管理
- [x] `DamageCalculator.java` — 伤害计算器
- [x] `ViewNotifier.java` — 后端→前端通知接口
- [x] `BuffResolver.java` — Buff 解析工具

## 阶段十：事件系统 ✓

- [x] `EventHandler.java` — 事件处理器（含相遇、雕像、黏液等全部事件）

## 阶段十一：数据层 ✓

- [x] `DataLoader.java` — JSON 数据加载器
- [x] `SaveManager.java` — 存档管理器
- [x] 存档保存/恢复藏品 ID 和药水 ID

## 阶段十二：商店系统 ✓

- [x] `ShopManager.java` — 商店逻辑
- [x] 商店卡牌、藏品、药水均使用真实对象
- [x] 购买藏品进入藏品栏，购买药水进入 3 格药水栏

## 阶段十三：View 层（LibGDX）✓

- [x] `FabricBookGame.java` — 入口
- [x] `TitleScreen.java` — 标题画面
- [x] `CharacterSelectScreen.java` — 角色选择
- [x] `MapScreen.java` — 地图探索
- [x] `BattleScreen.java` — 战斗场景
- [x] `ShopScreen.java` — 商店
- [x] `EventScreen.java` — 事件
- [x] `CardActor.java` — 卡牌 Actor
- [x] `EnemyActor.java` — 怪物 Actor
- [x] `PlayerActor.java` — 玩家 Actor
- [x] `ButtonActor.java` — 按钮
- [x] `HandPanel.java` — 手牌面板
- [x] `EnergyBar.java` — 能量条
- [x] `BuffBar.java` — Buff 状态栏
- [x] 战斗界面支持使用药水

## 阶段十四：JSON 数据配置 ✓

- [x] `data/cards/warrior.json` — 战士卡牌数据
- [x] `data/monsters/level1~5.json` — 5 层全部敌人 + Boss
- [x] `data/relics.json` — 45 个藏品（7 类稀有度）
- [x] `data/maps/levels.json` — 5 层地图配置
- [x] `data/potions.json` — 10 种药水
- [x] `data/events.json` — 8 个事件

## 阶段十五：集成测试 ☐

- [x] Gradle 构建验证（`./gradlew build`）
- [x] 后端调试报告已覆盖地图、战斗、事件、跨层通关主链路
- [ ] 自动化测试源码仍为空（Gradle 显示 `NO-SOURCE`）
- [ ] 前端完整点击/拖拽流程仍需人工或 UI 自动化验收

## 当前剩余重点

- [ ] 将 `MapScreen` 内置的原版地图常量逐步下沉到 `MapConfig` / JSON。
- [ ] 将法师、女巫从试作职业扩展为完整卡池和初始牌组。
- [ ] 为复杂藏品补专用实现，例如环境/结局/特殊敌人相关效果。
- [ ] 建立最小单元测试：药水使用、藏品即时效果、伤害修正、商店购买、存档恢复。
