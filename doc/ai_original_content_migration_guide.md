# AI 原版内容迁移指南

本文档用于指导 AI Agent 将原版 `mcjiansheng/FabricatedBook` 的怪物、卡牌、藏品、药水、地图与事件内容迁移到本 Java + LibGDX 重制版中。

目标不是机械复制文本，而是把原版 C + SDL2 项目的内容整理成当前 Java 项目能运行、能扩展、能测试的数据和代码。

---

## 1. 项目定位

当前项目是 `FabricatedBook` 的 Java 重制版：

- 原版：C + SDL2，课程项目，玩法类似《杀戮尖塔》。
- 重制版：Java 17 + LibGDX。
- 核心目标：逻辑层和表现层分离，后续方便扩展职业、卡牌、敌人、藏品、事件和地图。

重制版核心分层：

- `core/src/main/java/com/fabricatedbook/core/`：纯逻辑层，战斗、实体、卡牌、Buff、藏品、地图、事件。
- `core/src/main/java/com/fabricatedbook/view/`：LibGDX 表现层。
- `core/src/main/resources/data/`：JSON 数据。
- `core/src/main/resources/img/`：图片资源。
- `doc/game_encyclopedia.md`：当前已整理出的游戏百科。
- `doc/original_map_analysis.md`：原版地图生成算法分析。

迁移内容时，优先维护逻辑层和 JSON，避免把内容写死进 UI。

---

## 2. 迁移前必须阅读的文件

请按顺序阅读：

1. `README.md`
2. `doc/game_encyclopedia.md`
3. `doc/original_map_analysis.md`
4. `core/src/main/java/com/fabricatedbook/core/card/Card.java`
5. `core/src/main/java/com/fabricatedbook/core/card/CardPool.java`
6. `core/src/main/java/com/fabricatedbook/core/engine/CombatEngine.java`
7. `core/src/main/java/com/fabricatedbook/core/action/BuffResolver.java`
8. `core/src/main/java/com/fabricatedbook/data/DataLoader.java`
9. `core/src/main/java/com/fabricatedbook/core/entity/Enemy.java`
10. `core/src/main/java/com/fabricatedbook/core/relic/Relic.java`
11. `core/src/main/java/com/fabricatedbook/core/relic/RelicManager.java`
12. `core/src/main/resources/data/cards/warrior.json`
13. `core/src/main/resources/data/monsters/level1.json`
14. `core/src/main/resources/data/relics.json`
15. `core/src/main/resources/data/potions.json`
16. `core/src/main/resources/data/events.json`

---

## 3. 当前数据接入现状

### 3.1 卡牌

已有 JSON：

- `core/src/main/resources/data/cards/warrior.json`

但当前运行时大量地方仍使用 `CardPool` 中硬编码卡牌：

- `CombatEngine.initPlayerDeck()` 通过 `CardPool.findById("war_atk1")` 和 `CardPool.findById("war_def1")` 创建初始牌组。
- `ShopManager.generateItems()` 使用 `CardPool.getCardsByProfession(profession)` 生成商店卡牌。
- `CombatEngine.add_random_attack` 也从 `CardPool` 抽攻击牌。

因此迁移卡牌时有两种方案：

1. 短期安全方案：同时更新 `data/cards/*.json` 和 `CardPool.register*Cards()`。
2. 长期推荐方案：改造 `CardPool`，启动时从 `DataLoader.loadCards(profession)` 注册 JSON 卡牌，然后删除重复硬编码。

如果只是补 JSON 而不处理 `CardPool`，很多新卡不会进入战斗和商店。

### 3.2 怪物

已有 JSON：

- `core/src/main/resources/data/monsters/level1.json`
- `core/src/main/resources/data/monsters/level2.json`
- `core/src/main/resources/data/monsters/level3.json`
- `core/src/main/resources/data/monsters/level4.json`
- `core/src/main/resources/data/monsters/level5.json`

`DataLoader.loadMonsters(level)` 能读取这些文件。

当前正式前端的 `MapScreen.createEnemiesFor(int nodeType)` 已从
`DataLoader.loadMonsters(currentLayerIdx + 1)` 按楼层和节点类型随机选怪物组。
如果当前层没有匹配组，才会 fallback 到训练敌人。

### 3.3 藏品

已有 JSON：

- `core/src/main/resources/data/relics.json`

当前藏品已通过 `RelicData`、`DataRelic`、`RelicFactory` 接入：

- `DataLoader.loadRelicData()` 读取 `data/relics.json`。
- `ShopManager` 从 JSON 藏品池生成真实 `Relic` 商品。
- `EventScreen` 会将事件结果里的 `relicId` 实例化为真实藏品。
- 战斗奖励可掉落真实藏品。
- `RelicManager` 负责获得藏品、战斗事件订阅、伤害/金币/治疗修正。

注意：当前只对通用效果和常见效果做了可运行实现。与结局、特殊敌人、复杂环境规则强绑定的藏品仍需要新增专用实现。

### 3.4 药水

已有 JSON：

- `core/src/main/resources/data/potions.json`

药水已通过 `core/potion/Potion.java` 接入：

- `DataLoader.loadPotions()` 读取 `data/potions.json`。
- `Player` 持有 3 格药水栏。
- `ShopManager` 购买药水后加入药水栏。
- `BattleScreen` 显示药水按钮，点击后执行效果并移除药水。
- 存档会保存/恢复药水 ID。

当前药水复用轻量 DSL，已支持 `heal`、`damage_all`、`block`、`energy`、`draw`、`buff:self`、`debuff_all`、`remove_all_enemy_block`、`trigger_withering_all`。

### 3.5 地图

已有 JSON：

- `core/src/main/resources/data/maps/levels.json`

但最新前端地图为了复刻原版，`MapScreen` 内部已经采用了原版 C 项目的列优先稀疏地图生成逻辑。后续建议把 `MapScreen` 内部常量逐步抽到 `MapConfig`，避免地图规则散落在 UI 层。

---

## 4. JSON 编写规范

### 4.1 通用规则

- 所有文件使用 UTF-8。
- 当前 Gson 设置了 `setLenient()`，现有 JSON 有注释，但推荐新增内容尽量保持标准 JSON，减少工具兼容问题。
- `id` 使用英文小写蛇形或短横，不要用中文。
- `name` 和 `description` 用中文。
- 所有效果写进 `effects` 或 `actionScript` 时，必须匹配 `CombatEngine` 当前支持的 DSL。

---

## 5. 卡牌迁移

### 5.1 卡牌 JSON 字段

路径：

- `core/src/main/resources/data/cards/warrior.json`
- 后续可新增 `mage.json`
- 后续可新增 `witch.json`

字段格式：

```json
{
  "id": "warrior_attack",
  "name": "攻击",
  "cost": 1,
  "description": "造成 6 点伤害",
  "type": "ATTACK",
  "rarity": "BASIC",
  "value": 0,
  "targetType": "SINGLE_ENEMY",
  "targetCount": 1,
  "effects": ["damage:6"],
  "exhaust": false,
  "retain": false,
  "ethereal": false,
  "profession": "warrior"
}
```

说明：

- `cost = -1` 表示 X 费。打出时消耗当前全部剩余能量，并将实际消耗量记为 X。
- `exhaust = true` 表示打出后进入消耗牌堆，不进入弃牌堆。
- `retain = true` 表示回合结束时保留在手牌中。
- `ethereal = true` 表示回合结束时若仍在手牌中，则进入消耗牌堆。

### 5.2 枚举取值

`type` 对应 `Card.CardType`：

- `ATTACK`
- `DEFENSE`
- `SKILL`
- `EQUIP`

`rarity` 对应 `Card.Rarity`：

- `BASIC`
- `COMMON`
- `UNCOMMON`
- `RARE`
- `EPIC`
- `LEGENDARY`

注意：`Relic.Rarity` 有 `MYTHIC/SPECIAL/CURSED`，但 `Card.Rarity` 当前没有 `MYTHIC`。

`targetType` 对应 `Card.TargetType`：

- `SINGLE_ENEMY`
- `ENEMY`
- `ALL_ENEMIES`
- `SELF`
- `ALL_ALLIES`

### 5.3 当前支持的卡牌 effect DSL

由 `CombatEngine.parseCardEffects` 解析：

```text
damage:N
damage:N:TIMES
damage_x:N
damage_all:N
damage_all:N:TIMES
block:N
heal:N
draw:N
energy:N
debuff:BuffName:STACK
debuff_all:BuffName:STACK
buff:self:BuffName:STACK
purify
counter:block
bonus_per_attack:N
bonus_low_hp:THRESHOLD:BONUS
detonate_withering:TIMES
double_poison
block_per_target:N
bonus_per_damage_taken:THRESHOLD:BONUS
add_random_attack
stun_chance:PERCENT
```

补充：

- `damage_x:N`：对单目标造成 X 段 N 点伤害，X 为该 X 费牌打出时消耗的实际能量。
- 当前消耗牌进入 `exhaustPile`，不会在本场战斗洗牌时进入抽牌堆；新战斗开始时会回到玩家牌组。

### 5.4 支持的 Buff 名称

由 `BuffResolver` 解析，不区分大小写：

```text
Fragile / fragile / 脆弱
BlockReduction / block_reduction / blockreduction / 易碎
Weak / weak / 虚弱
Dizziness / dizzy / 眩晕
Poison / poisoning / 中毒
Withering / 凋零
Strength / 力量
Resistance / 抗性
BlockIncrease / block_increase / blockincrease / 坚强
Armor / ArmorBuff / armor / 装甲
Undead / UndeadBuff / undead / 不死
ExtraEnergy / ExtraEnergyBuff / extra_energy / extraenergy / 额外能量
```

### 5.5 必须修正的现有不兼容 effect

迁移时需要确认效果名和 `CombatEngine` 支持的 DSL 匹配：

- `damage_per_attack:10:3` 当前不支持。应改为 `damage:10` + `bonus_per_attack:3`，或扩展 `CombatEngine` 支持 `damage_per_attack`。
- `damage_scaling:7` 当前不支持。可改为 `damage:7` + `escalating:1`；`escalating` 已由卡牌效果解析器支持。
- 装甲、不死、额外能量已由 `BuffResolver` 支持，可使用 `Armor`/`ArmorBuff`、`Undead`/`UndeadBuff`、`ExtraEnergy`/`ExtraEnergyBuff` 等别名。
- 持续额外能量使用 `buff:self:extra_energy:持续回合:每回合能量`，例如搏命挣扎为 `buff:self:extra_energy:3:2`。
- `trigger_withering_all`、`remove_all_enemy_block` 等药水效果在药水解析中支持；卡牌侧如需全体版本，应同步扩展 `CombatEngine.parseCardEffects` 并写测试。

迁移时不要新增没有解析器支持的 effect；如果必须新增，要同步改 `CombatEngine.parseCardEffects` 并写测试。

### 5.6 新职业卡牌

新增职业时需要改：

1. `core/src/main/java/com/fabricatedbook/core/entity/Profession.java`
2. `core/src/main/resources/data/cards/{profession}.json`
3. `CardPool` 的注册逻辑，或改为从 JSON 统一加载。
4. `CharacterSelectScreen` 的文案和立绘。
5. `PlayerActor.loadSprite()` 中的职业贴图映射。

---

## 6. 怪物迁移

### 6.1 怪物 JSON 字段

路径：

- `core/src/main/resources/data/monsters/level1.json`
- `level2.json`
- `level3.json`
- `level4.json`
- `level5.json`

格式：

```json
{
  "id": "w1_scavenger",
  "name": "拾荒者",
  "isBoss": false,
  "floor": 1,
  "enemies": [
    {
      "id": "scavenger",
      "name": "拾荒者",
      "maxHp": 45,
      "actionScript": ["atk6", "def5", "atk8"],
      "passive": ""
    }
  ]
}
```

一个 `EnemyGroup` 表示一场战斗，`enemies` 内可有多个敌人。
`passive` 字段会保留到 `Enemy` 实体，并由 `CombatEngine` 在战斗开始、回合开始、敌人攻击后和死亡检查阶段结算。

### 6.2 当前支持的敌人 actionScript

当前主入口是 `EnemyActionResolver`。`CombatEngine` 会优先调用 resolver；resolver 未识别时才回退到旧基础 DSL。现有 `level1` 到 `level5` 的 JSON 保留原版风格 actionId，例如：

```text
atk_double_3
inc_strength_3
atk_debuff_blockred
def_block_10
atk_wither_strike
def_team_shield
heal_emergency
trigger_wither_puppet
```

旧基础 DSL 仍可兼容简单动作：

```text
atkN / attackN      对玩家造成 N 点伤害
atkNxM / attackNxM  对玩家造成 N 点伤害，重复 M 次
defN / blockN       敌人获得 N 格挡
idle / stun         无行动
```

### 6.3 当前校验

现有怪物 action 兼容性已由自动化测试固定：

```bash
./gradlew :core:test --tests com.fabricatedbook.core.engine.EnemyActionResolverTest
printf 'selftest\nquit\n' | ./gradlew runBackendDebug
```

如果新增 JSON action 未接入 resolver，测试和后端 CLI 会打印具体的 `level/group/enemy -> actionId`。

### 6.4 推荐的敌人 action 设计

新增怪物时优先沿用可读的语义化 id，而不是把复杂行为塞进基础字符串解析：

```text
atk_*
def_*
buff_* / inc_*
curse_*
heal_*
steal_block
transfer_curse
```

新增 actionId 必须同步：

- `EnemyActionResolver.resolve(...)`：登记实际动作。
- `EnemyActionResolver.describeIntent(...)`：补玩家可读意图。
- `doc/enemy_action_dsl.md`：如新增一类命名约定，同步说明。
- `EnemyActionResolverTest` 和 CLI `selftest`。

### 6.5 怪物图片映射

敌人立绘在：

- `core/src/main/resources/img/`

显示映射在：

- `core/src/main/java/com/fabricatedbook/view/actor/EnemyActor.java`

如果新增敌人或改中文名，需要更新 `NAME_TO_FILE`：

```java
NAME_TO_FILE.put("拾荒者", "ragpicker");
```

---

## 7. 藏品迁移

### 7.1 当前 relics.json 字段

```json
{
  "id": "relic_hot_water_flask",
  "name": "热水壶",
  "rarity": "COMMON",
  "description": "生命值上限 +5",
  "effectValue": 5
}
```

### 7.2 当前问题

`Relic` 是接口，Gson 不能直接实例化。要让 JSON 藏品真正生效，必须新增数据类和工厂。

推荐新增：

```java
public class RelicData {
    public String id;
    public String name;
    public Relic.Rarity rarity;
    public String description;
    public String effectType;
    public int effectValue;
}
```

然后修改 JSON：

```json
{
  "id": "relic_hot_water_flask",
  "name": "热水壶",
  "rarity": "COMMON",
  "description": "生命值上限 +5",
  "effectType": "max_hp",
  "effectValue": 5
}
```

### 7.3 推荐 effectType

先覆盖原版常见藏品：

```text
max_hp
gain_gold_now
combat_start_energy
combat_start_draw
damage_percent
target_damage_taken_percent
shop_enter_gold
card_used_heal
turn_start_damage_all
low_hp_damage_percent
low_hp_block_each_turn
combat_victory_gold
combat_victory_heal
poison_withering_damage_percent
negative_relic_damage_reduce
```

若某个藏品效果很特殊，优先新增具体 `Relic` 实现，而不是用巨大 switch 硬塞。

### 7.4 推荐代码结构

新增：

```text
core/src/main/java/com/fabricatedbook/core/relic/RelicData.java
core/src/main/java/com/fabricatedbook/core/relic/DataRelic.java
core/src/main/java/com/fabricatedbook/core/relic/RelicFactory.java
```

`DataRelic` 可以实现简单通用效果；特殊效果用单独类。

`RelicFactory` 负责：

- 从 `DataLoader` 获取 `RelicData`
- 根据 `effectType` 生成 `DataRelic` 或具体类
- 提供随机按稀有度抽取

同时修改：

- `DataLoader.loadRelics()` 改为返回 `List<RelicData>` 或新增 `loadRelicData()`
- `ShopManager.generateItems()` 使用真实藏品池
- `ShopManager.purchase()` 购买 RELIC 时调用 `relicManager.addRelic(relic)`

---

## 8. 药水迁移

### 8.1 当前 potions.json 字段

```json
{
  "id": "potion_heal",
  "name": "回血药水",
  "description": "回复 10 点生命值",
  "effects": ["heal:10"]
}
```

### 8.2 推荐新增结构

新增：

```text
core/src/main/java/com/fabricatedbook/core/potion/Potion.java
core/src/main/java/com/fabricatedbook/core/potion/PotionFactory.java
```

`Potion` 字段：

```java
public class Potion {
    private String id;
    private String name;
    private String description;
    private List<String> effects;
}
```

药水效果可复用卡牌 effect DSL，但要注意：

- 药水没有能量费用。
- 药水不进入手牌/弃牌堆。
- 药水使用后从药水槽移除。
- 药水可能在战斗中使用，也可能只能在战斗中使用。

---

## 9. 地图和节点迁移

原版地图的关键是：

- 列优先稀疏网格。
- 每列节点数随机。
- 起点和终点固定单节点。
- 中间列用滑动窗口连接。
- 第 4 层有特殊 Boss -> Decision 结构。

迁移时不要把它改回完整矩形网格。

当前 `MapScreen` 已临时内置原版参数：

```java
LAYER_LENGTHS
LAYER_WIDTHS
LAYER_START_TYPES
LAYER_END_TYPES
LAYER_PROBABILITIES
```

后续推荐：

- 把这些配置移动到 `data/maps/levels.json`
- 用 `MapGraph` 或新 `LayerGraph` 表示稀疏列图
- `MapScreen` 只负责渲染和点击，不负责生成规则

---

## 10. 事件迁移

请检查：

- `core/src/main/java/com/fabricatedbook/core/event/EventHandler.java`
- `core/src/main/resources/data/events.json`

如果当前 `EventHandler` 仍硬编码事件，应改造为：

- JSON 定义事件标题、描述、选项。
- Java 定义 `EventResult` 的可执行效果。
- 对特殊事件保留 Java handler。

推荐 JSON：

```json
{
  "id": "lost_traveler",
  "name": "迷途旅人",
  "description": "你遇到了一位迷路的旅人。",
  "options": [
    {
      "label": "帮助他",
      "description": "失去 5 生命，获得 30 金币",
      "effects": ["hp:-5", "gold:30"]
    }
  ]
}
```

---

## 11. 迁移工作顺序

推荐分阶段提交，不要一次性改完所有系统。

### 阶段 A：卡牌一致性

1. 对照原版卡牌，修正 `warrior.json`。
2. 修正 `CardPool` 与 JSON 不一致的问题。
3. 让 `CardPool` 从 JSON 注册卡牌，或至少同步硬编码。
4. 修正不支持的 effect。
5. 编译并跑基础战斗。

验收：

- 初始牌组能生成。
- 商店能刷出正确职业卡牌。
- 每张卡使用时没有 `[CombatEngine] 未知效果`。

### 阶段 B：怪物

1. 对照原版怪物和楼层，补齐 `level1~5.json`。
2. 修正 `actionScript` 为当前可解析格式，或实现 `EnemyActionResolver`。
3. 修改 `MapScreen.createEnemiesFor` 从 JSON 随机读取。
4. 更新 `EnemyActor.NAME_TO_FILE` 图片映射。

验收：

- 普通战斗、精英战、Boss 战能从对应楼层生成真实敌人。
- 敌人意图显示正常。
- 敌人行动不 fallback 为默认 6 点攻击。

### 阶段 C：藏品

1. `RelicData/DataRelic/RelicFactory` 已完成。
2. `DataLoader.loadRelicData()` 已完成。
3. `ShopManager`、事件结果、战斗奖励已使用真实藏品。
4. 常见藏品已接入事件总线和战斗修正；复杂藏品继续补专用类。

验收：

- 商店购买藏品后进入玩家藏品列表。
- 战斗开始/用牌/回合结束/击杀/战斗结束事件能触发藏品。

### 阶段 D：药水

1. `Potion` 数据类已完成。
2. 玩家 3 格药水槽已完成。
3. 商店购买药水进入药水槽已完成。
4. 战斗 UI 可使用药水已完成。

### 阶段 E：事件和地图层效果

1. 事件 JSON 化。
2. 层环境效果接入移动/战斗奖励/伤害修正。
3. Boss 后进入下一层或结局。

---

## 12. 代码注意事项

### 12.1 不要回退用户已有修改

本项目工作区可能有未提交修改。修改前先看：

```bash
git status --short
```

只改迁移需要的文件，不要格式化全项目。

### 12.2 编译验证

每阶段至少运行：

```bash
./gradlew compileJava desktop:compileJava
```

如果 Gradle 因为 `~/.gradle` wrapper cache 权限失败，在 Codex 环境中需要请求授权；在用户本机终端直接运行即可。

### 12.3 搜索未知 effect

迁移后要检查运行日志中是否有：

```text
[CombatEngine] 未知效果
[CombatEngine] 解析敌人动作失败
[BuffResolver] 未知 Buff 名称
```

也可以用静态 grep 找出疑似未支持 effect：

```bash
rg '"effects"|actionScript|damage_per_attack|damage_scaling|energy_per_turn|trigger_withering|remove_all_enemy_block' core/src/main/resources/data
```

---

## 13. AI Agent 迁移核查提示词

如果要让 AI Agent 接手内容迁移或状态核查，请在项目根目录运行对应工具，并把本文档作为上下文。下面提示词适用于继续迁移原版内容、核查新 JSON、或确认现有状态是否过期；它不是当前已完成工作的重复修复清单。

可直接粘贴：

```text
你现在位于 Java + LibGDX 项目 FabricatedBook_Remake。
这是原版 C + SDL2 游戏 FabricatedBook 的 Java 重制版。

请先阅读这些文件：
- README.md
- doc/game_encyclopedia.md
- doc/original_map_analysis.md
- doc/ai_original_content_migration_guide.md
- core/src/main/java/com/fabricatedbook/core/card/Card.java
- core/src/main/java/com/fabricatedbook/core/card/CardPool.java
- core/src/main/java/com/fabricatedbook/core/engine/CombatEngine.java
- core/src/main/java/com/fabricatedbook/core/action/BuffResolver.java
- core/src/main/java/com/fabricatedbook/data/DataLoader.java
- core/src/main/java/com/fabricatedbook/view/screen/MapScreen.java
- core/src/main/resources/data/cards/warrior.json
- core/src/main/resources/data/monsters/level1.json
- core/src/main/resources/data/monsters/level2.json
- core/src/main/resources/data/monsters/level3.json
- core/src/main/resources/data/monsters/level4.json
- core/src/main/resources/data/monsters/level5.json

任务：
1. 不要重构 UI，不要大面积格式化。
2. 先核查当前代码和本文档中的已完成状态是否一致。
3. 迁移新卡牌时，保证 JSON effect 能被 CombatEngine 正确解析；如需新增 effect，优先在核心逻辑小范围实现，并保持旧 effect 兼容。
4. 迁移新怪物时，优先把语义化 actionId 接入 EnemyActionResolver，并同步 doc/enemy_action_dsl.md、EnemyActionResolverTest 和后端 CLI selftest。
5. 保持敌人遭遇选择通过 EnemyEncounterResolver 从 DataLoader.loadMonsters(currentLayerIdx + 1) 读取对应楼层怪物组；如果调整地图或节点类型，必须同步前端生成逻辑和测试文档。
6. 新增敌人图片时，同步 EnemyActor.NAME_TO_FILE，保证怪物名称能匹配 img 目录中贴图。
7. 每完成一个阶段运行 ./gradlew test，并用 printf 'selftest\nseedtest 12345\nsavetest\nflowtest\nroutetest\nquit\n' | ./gradlew runBackendDebug 做后端 CLI 回归。
8. 最后汇报改了哪些文件、哪些效果或动作仍未完全实现、下一步建议。

约束：
- 保留用户已有未提交修改，不要 git reset，不要回退无关文件。
- 数据写入 core/src/main/resources/data。
- Java 代码只改 core/src/main/java 下与数据接入相关的类。
- 不要把怪物和卡牌继续写死进 UI。
```

---

## 14. 当前建议的第一批具体修复清单

第一批“原版内容进入游戏循环”的接入项当前状态：

1. 修 `warrior.json` 中不支持的 effect。
2. 改 `CardPool` 从 JSON 注册，或至少同步 JSON 与硬编码 ID。
3. `MapScreen.createEnemiesFor` 已使用 `DataLoader.loadMonsters`。
4. `EnemyActionResolver` 已覆盖现有 `level*.json` 的主要 actionScript。
5. `ShopManager` 已使用真实卡牌、藏品和药水对象。

这五项完成后，原版的卡牌、怪物、藏品和药水已经进入 Java 重制版主循环；后续重点是继续补复杂藏品/环境规则、前端验收流程和地图文档统一。
