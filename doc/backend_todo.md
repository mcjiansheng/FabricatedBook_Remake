# 后端专项 TODO：结构问题与修复顺序

> 更新时间：2026-06-26  
> 目的：先把后端结构问题、影响范围和推荐修复顺序说清楚，再进入具体实现。本文只记录后端专项判断，不替代根目录 `TODO.md` 的总 Backlog。

## 总体判断

当前后端主链路可以运行，`CombatEngine`、`ActionManager`、`CombatAction`、`DamageCalculator`、`BuffHook`、`RelicManager`、`SaveManager` 和 `BackendDebugLauncher` 已经形成基础架构。现阶段问题不是“完全缺后端”，而是有几处规则源分散、状态归属不清和个别回调错误。

推荐修复顺序：

1. 修正战斗胜负回调错误。
2. 将商店弃牌次数从静态状态迁到对局状态或玩家长期状态。
3. 统一卡牌来源和职业初始牌组创建方式。
4. 下沉地图生成、节点进入和环境效果规则。
5. 收敛卡牌 effect DSL 的执行与预览解析。
6. 决定事件系统是否继续 Java handler，还是逐步 JSON/DSL 化。

## B-001：战斗胜利回调触发位置错误

状态：已修复。`CombatEngine.checkBattleEnd()` 已调整为失败只清理战斗，胜利时触发 `RelicManager.onCombatVictory()` 后再清理订阅；已补充胜利/失败两个方向的核心测试。

### 位置

- 包：`com.fabricatedbook.core.engine`
- 类：`CombatEngine`
- 方法：`checkBattleEnd()`
- 相关类：
  - `com.fabricatedbook.core.relic.RelicManager`
  - `com.fabricatedbook.core.relic.DataRelic`

### 问题

`checkBattleEnd()` 的玩家死亡分支调用了 `relicManager.onCombatVictory()`，而敌人全灭的胜利分支只调用 `relicManager.onCombatEnd()`，没有调用 `onCombatVictory()`。

这会导致胜利累计类藏品在失败时累计，在真实胜利时不累计。例如 `DataRelic` 中 `relic_frostmourne` 通过 `combatWins` 计算伤害成长，但 `combatWins` 的增长依赖 `onCombatVictory()`。

### 期望实现

- 玩家死亡：只执行战斗结束清理和失败通知。
- 敌人全灭：先结算胜利奖励和胜利类藏品，再执行战斗结束清理。
- 胜利事件、死亡事件、奖励结算和订阅清理的顺序应稳定，并用测试固定下来。

### 解决方式

1. 从失败分支移除 `relicManager.onCombatVictory()`。
2. 在胜利分支中调用 `relicManager.onCombatVictory()`。
3. 补一个核心单元测试：玩家死亡时不增长胜利计数，敌人全灭时增长。

### 影响面

- 影响藏品胜利累计逻辑。
- 可能改变已有战斗奖励或藏品数值表现，但这是修正错误行为。
- 应运行 `./gradlew test` 和后端 CLI `selftest`、`seedtest`、`savetest`。

## B-002：商店弃牌次数使用 JVM 静态状态

状态：已修复。弃牌次数已迁入 `GameRunState.shopRemoveCount`，正式商店通过 `GameRunState` 读写该计数；`SaveManager` 已保存/恢复该字段，并支持测试传入临时存档路径。旧的无对局构造器仅保留实例内计数，不再使用 JVM 静态状态。

### 位置

- 包：`com.fabricatedbook.core.shop`
- 类：`ShopManager`
- 字段：`static int totalRemoveCount`
- 方法：
  - `generateItems()`
  - `purchaseRemove(int cardIndex)`
- 相关类：
  - `com.fabricatedbook.core.run.GameRunState`
  - `com.fabricatedbook.data.SaveManager`
  - `com.fabricatedbook.core.entity.Player`

### 问题

弃牌服务价格使用 `static totalRemoveCount` 计算。这个状态属于 JVM 进程，不属于某个玩家或某次对局。

问题表现：

- 新对局可能继承旧对局的弃牌次数。
- 读档无法恢复真实弃牌次数。
- 单元测试或 CLI 多次运行时可能互相污染。
- 多玩家、多存档或未来多槽位存档无法隔离。

### 期望实现

弃牌次数应是对局长期状态，随保存和读取恢复。它不应该放在 `ShopManager` 的静态字段上。

推荐归属：

- 如果弃牌价格是本次对局级成长：放入 `GameRunState`。
- 如果弃牌次数跟随玩家长期构筑：放入 `Player`。

当前项目已有 `GameRunState` 承载 seed、节点、战斗快照，且 `SaveManager.saveRun()` 已保存对局状态，因此更推荐放入 `GameRunState`，并由 `ShopManager` 构造时接收或通过独立服务读取。

### 解决方式

1. 新增对局字段，例如 `shopRemoveCount`。
2. `SaveManager.SaveData`、`saveRun()`、`loadRun()` 同步保存和恢复该字段。
3. `ShopManager` 不再持有静态计数，改为读取当前对局计数。
4. `purchaseRemove()` 成功后更新对局计数。
5. 补测试：新对局价格从 75 开始；读档后价格保持；两个对局互不影响。

### 影响面

- 会影响 `ShopScreen` 构造 `ShopManager` 的方式。
- 会影响存档格式，需要考虑版本兼容：旧存档没有该字段时默认 0。
- 后端 CLI 当前商店是轻量占位，若后续接入真实商店也应复用同一状态。

## B-003：卡牌数据源与职业初始牌组没有统一

状态：已修复战士主链路。已新增 `StarterDeckFactory`，由 `GameRunState` 在新对局创建时初始化长期初始牌组；`CombatEngine` 不再硬编码创建 `war_atk1` / `war_def1` / `war_painful_blow`，CLI 调试新对局和种子测试也复用同一入口。`warrior.json` 已改为运行时 `war_*` ID 并补齐升级字段，`CardPool` 现在优先从 JSON 注册战士卡，失败时才回退到旧硬编码。法师/女巫完整卡池仍归入职业完整化任务。

### 位置

- 包：`com.fabricatedbook.core.engine`
- 类：`CombatEngine`
- 方法：`initPlayerDeck()`
- 包：`com.fabricatedbook.core.card`
- 类：
  - `CardPool`
  - `CardFactory`
- 包：`com.fabricatedbook.data`
- 类：`DataLoader`
- 相关资源：
  - `core/src/main/resources/data/cards/warrior.json`

### 问题

当前战斗初始牌组在 `CombatEngine.initPlayerDeck()` 中硬编码：

- `war_atk1` x5
- `war_def1` x4
- `war_painful_blow` x1

同时，`CardPool` 内部也硬编码战士卡牌；JSON 里的 `warrior.json` 又是另一份数据。这样会出现三个问题：

- 职业扩展困难：法师、女巫无法自然生成自己的初始牌组。
- 数据不一致风险：JSON 修改后，战斗、商店、奖励可能仍使用硬编码 `CardPool`。
- 职责不清：`CombatEngine` 应负责战斗流程，不应知道某职业有哪些初始牌。

### 期望实现

卡牌应有单一运行时来源。推荐长期形态：

- `DataLoader` 从 JSON 加载卡牌。
- `CardPool` 只做注册、检索和按职业筛选，不再硬编码完整卡池。
- 职业初始牌组配置由职业数据或卡牌 JSON 中的标签表达。
- `CombatEngine` 只读取玩家已有长期牌组；如果新游戏需要创建初始牌组，应由新对局创建流程或职业工厂完成。

### 解决方式

分阶段处理更稳：

1. 短期：新增 `StarterDeckFactory` 或 `ProfessionDeckFactory`，把 `initPlayerDeck()` 中硬编码挪出 `CombatEngine`。
2. 中期：让 `CardPool` 启动时从 `DataLoader.loadCards(profession)` 注册 JSON 卡牌。
3. 长期：删除或降级 `CardPool.registerWarriorCards()` 的硬编码兜底。
4. 为三职业新游戏、商店、奖励、随机攻击牌补测试。

### 影响面

- 影响新游戏创建、战斗初始化、商店商品、奖励三选一、存档恢复。
- 存档中只保存卡牌 ID 和升级态，因此统一 ID 后可以兼容；如果修改 ID，需要迁移旧存档。
- `BattleScreen` 的奖励生成也依赖 `CardPool`，需要一起检查。

## B-004：地图规则有两套实现，正式前端没有复用 core 地图

状态：已修复地图生成主链路。已新增 `LayerMapConfig`、`LayerMapGraph`、`LayerMapNode`，用 core 表达正式前端使用的列优先稀疏地图；`data/maps/levels.json` 已改为稀疏图配置，`DataLoader.loadLayerMapConfigs()` 负责加载并在失败时回退默认配置。`MapScreen.generateLayer()` 从 `LayerMapGraph` 复制节点和连线用于渲染，`BackendDebugLauncher` 也使用同一 core 图。迷雾层现在由 JSON/core 固定为倒数第二列 Boss、最后一列命运抉择，并有单元测试与 CLI `selftest` 覆盖。剩余的节点进入、副作用和环境效果统一归入 B-005。

### 位置

- 包：`com.fabricatedbook.core.map`
- 类：
  - `MapConfig`
  - `MapGraph`
  - `NodeFactory`
  - `Node`
- 包：`com.fabricatedbook.view.screen`
- 类：`MapScreen`
- 方法：
  - `generateAllLayers()`
  - `generateLayer(int layerIdx)`
  - `connectColumns(...)`
  - `enterNode(MapNode node)`
  - `markNodeEntered(MapNode node)`
  - `applyNodeEntryRelics(int nodeType)`

### 问题

`core.map` 中有矩形网格式 `MapGraph`，后端 CLI 使用它调试地图；正式前端 `MapScreen` 里又内置了一套原版稀疏列地图，包括：

- `LAYER_LENGTHS`
- `LAYER_WIDTHS`
- `LAYER_START_TYPES`
- `LAYER_END_TYPES`
- `LAYER_PROBABILITIES`
- 迷雾层倒数第二列 Boss
- 节点连接算法

结果是 CLI 与玩家实际地图不是同一套规则。文档和测试报告中也记录了这个边界。

### 期望实现

地图规则应在 core 中有唯一实现，前端只负责渲染和输入。

推荐形态：

- 新增或扩展一个支持稀疏列图的 core 模型，例如 `RunMapGraph` / `LayerGraph`。
- 地图配置从 `data/maps/levels.json` 读取，能表达每层长度、最大宽度、起点、终点、特殊列、节点概率和环境规则。
- `MapScreen` 持有 core 地图对象，只做节点位置布局、绘制和点击。
- `BackendDebugLauncher` 与 `MapScreen` 复用同一个地图生成器和移动规则。

### 解决方式

1. 先确认保留 `MapGraph` 并扩展，还是新增稀疏图模型。
2. 把 `MapScreen` 中的层级常量迁到 `MapConfig` 或新的配置 DTO。
3. 把 `generateLayer()` 和 `connectColumns()` 下沉到 core。
4. 给 core 地图补 seed 可复现测试、连接合法性测试、迷雾 Boss -> 命运抉择结构测试。
5. 改 `MapScreen` 读取 core 地图对象并渲染。
6. 改 CLI 使用同一地图对象。

### 影响面

- 影响地图界面、后端 CLI、存档节点坐标、事件/商店/战斗入口。
- `GameRunState.NodeRef` 当前保存 layer、col、row、type；稀疏图继续使用这些字段可以降低迁移成本。
- 需要检查旧存档：如果地图生成算法变化，同 seed 同坐标可能指向不同节点。近期可接受重新生成，或增加地图版本。

## B-005：环境效果和部分节点进入规则仍在 UI 层或仅有描述

状态：部分修复。已新增 `NodeEntryResolver` / `NodeEntryResult` 作为 core 节点进入规则入口，`relic_oligarch` 的“进入非战斗节点获得金币”已从 `MapScreen` 下沉到 core，并由 `MapScreen` 与 `BackendDebugLauncher` 共用；森林非战斗节点扣 10-20 金币、诡异秘林进入战斗/非战斗调整伤害修正（±3）、迷雾每次前进随机回血/扣血已在 core 节点进入入口执行，且随机绑定 seed+节点坐标。诡异秘林伤害修正进入 `GameRunState.mapDamageModifier` 并随存档保存恢复，`CombatEngine` 和 `CombatPreviewCalculator` 都会应用到玩家伤害。`NodeEntryResult` 消息现在会传入商店、事件、安全屋和战斗页面，避免前端吞掉环境/藏品反馈。`GameRunState.beginNode()` 已作为战斗/非战斗共用的节点开始入口，所有节点在入口副作用前记录节点前快照；`SaveManager.saveRun()` 会在 active 节点中保存该快照，读档后不保留未完成节点过程状态，也不会把事件/商店/安全屋等非战斗节点提前标记完成。测试覆盖寡头触发、三层环境效果、存档恢复、战斗伤害、预览接入，以及战斗/非战斗节点中途存档不会写入未完成过程。剩余工作：继续梳理各 UI 操作中的自动保存入口，确认节点内购买/事件选择/奖励领取等玩家可见流程的保存粒度。

### 位置

- 包：`com.fabricatedbook.core.map`
- 类：`MapConfig`
- 字段：`environmentEffect`
- 包：`com.fabricatedbook.view.screen`
- 类：`MapScreen`
- 方法：
  - `enterNode(MapNode node)`
  - `markNodeEntered(MapNode node)`
  - `applyNodeEntryRelics(int nodeType)`
- 相关类：
  - `RelicManager`
  - `DataRelic`
  - `GameRunState`

### 问题

`MapConfig.environmentEffect` 是文本描述，不是可执行规则。森林、诡异秘林、迷雾等层环境效果尚未成为 core 规则。部分节点进入效果，例如 `relic_oligarch` 在非战斗节点获得金币，目前写在 `MapScreen.applyNodeEntryRelics()`。

这会让前端和 CLI 行为不一致，也让存档、预览和测试难以覆盖。

### 期望实现

节点进入应经过 core 规则入口，而不是由 `MapScreen` 直接修改玩家状态。

推荐形态：

- 新增 `RunRuleService`、`NodeResolver` 或类似 core 服务。
- 输入：`GameRunState`、节点引用、节点类型。
- 输出：节点进入结果，例如金币变化、生命变化、是否进入战斗、是否进入商店、事件名、环境反馈文本。
- 层环境、节点特性、藏品触发统一在这个入口结算。

### 解决方式

1. 先把“进入节点”的副作用列清楚：环境效果、寡头、森林金币、迷雾回血/扣血、战斗前快照、自动保存时机。
2. 在 core 建立节点进入结果对象。
3. `MapScreen` 调用 core 服务，根据结果切屏或展示反馈。
4. CLI 复用同一服务。
5. 补环境效果和藏品节点触发测试。

### 影响面

- 影响 `MapScreen`、`BackendDebugLauncher`、`SaveManager` 自动保存时机。
- 环境效果如果进入存档，需要扩展 `GameRunState`。
- 数值预览若要显示层环境修正，也需要同步 `CombatPreviewCalculator` 或新的地图预览服务。

## B-006：卡牌 effect DSL 执行与预览重复解析

状态：部分修复。已新增 `CardEffectParser` / `CardEffect`，`CombatEngine.parseCardEffects()` 和 `CombatPreviewCalculator.previewCard()` 现在共用同一个 DSL 拆分入口，不再各自处理空值、split 和 effect type 归一化；`CardEffectType` 已把现有 effect DSL 列成注册表，并记录实战执行、数值预览支持状态、DSL 参数数量范围、整数参数位置和关键文字参数约束。卡牌数值预览 switch 已从 `CombatPreviewCalculator` 抽到 `CardEffectPreviewer`，`CombatPreviewCalculator` 保留入口、目标准备和描述格式化职责；卡牌实战执行 switch 已从 `CombatEngine` 抽到 `CardEffectExecutor`，`CombatEngine` 只负责传入玩家、敌人、随机源和耗能上下文。后端 CLI `selftest` 已扫描所有已配置职业 JSON 卡牌 effect，遇到未知 DSL type、未接入实战执行的 type、参数数量不匹配、整数参数无法解析或文字参数不支持会失败并打印职业与卡牌 ID。剩余工作：继续把 executor/previewer 里的 switch 收敛为注册式分派，减少新增 effect 时的重复分支。

### 位置

- 包：`com.fabricatedbook.core.engine`
- 类：
  - `CombatEngine`
  - `CombatPreviewCalculator`
- 方法：
  - `CombatEngine.parseCardEffects(...)`
  - `CombatPreviewCalculator.previewCard(...)`
- 相关类：
  - `CombatAction`
  - `BuffResolver`
  - `DamageCalculator`

### 问题

实际结算和 UI 数值预览分别解析 `card.effects`。新增效果时，如果只改 `CombatEngine`，预览可能不准；如果只改预览，实际战斗不会生效。

当前已有一些默认分支会忽略或打印未知效果，短期可运行，但长期扩展卡牌时容易出现“卡牌能打但预览错”或“预览有但实际没有”的问题。

### 期望实现

卡牌效果解析应尽量有单一中间表示。预览和执行可以走不同执行器，但不应各自用字符串 switch 维护全部语义。

推荐形态：

- `CardEffectParser`：把字符串 DSL 解析为 `CardEffect` 对象。
- `CardEffectExecutor`：把 `CardEffect` 变成 `CombatAction` 或直接执行特殊效果。
- `CardEffectPreviewer`：读取同一个 `CardEffect` 计算预览。
- 未支持的 effect 在数据自检中失败，而不是静默进入游戏。

### 解决方式

1. 先把现有支持的 effect 列成枚举或注册表。（已完成：`CardEffectType`）
2. 为每个 effect 增加执行、预览支持状态和参数校验。（已完成：实战执行、数值预览、arity 与整数参数位置分开记录）
3. 把 `CombatEngine.parseCardEffects()` 拆出 parser。（已完成：`CardEffectParser`）
4. 让 `CombatPreviewCalculator` 使用 parser 的结果。（已完成）
5. 扩展 `selftest`：扫描已配置职业 JSON 卡牌，发现未知 effect 或未接入实战执行的 effect 直接报错。（已完成）
6. 下一步再把 `CombatEngine` 的执行 switch 和 `CombatPreviewCalculator` 的预览 switch 拆到专用 executor/previewer，减少继续扩展职业卡牌时的重复分支。（已完成：执行 switch 已迁入 `CardEffectExecutor`，预览 switch 已迁入 `CardEffectPreviewer`）

### 影响面

- 影响所有卡牌效果、战斗预览、测试。
- 需要保持现有 DSL 字符串兼容，否则会影响 JSON 数据和存档中的卡牌 ID 对应模板。
- 这是中等风险重构，应在 B-001、B-002 之后做。

## B-007：事件系统仍是 Java 硬编码，数据文件没有成为规则来源

状态：部分修复。`DataLoader` 已新增 `loadEvents()`，普通事件的名称、描述和选项展示现在由 `events.json` 驱动；`events.json` 也开始承载固定事件结果字段（`outcomeDescription`、`goldChange`、`hpChange`、`fullHeal`、`relicId`、`outcome`）和简单随机范围字段（`goldChangeMin/Max`、`hpChangeMin/Max`），复杂随机选项则用 `executor: "java"` 显式标记。`EventResultResolver` 会把可执行 JSON 选项转换为 `EventResult`，`EventHandler` 只负责优先调度数据结果再回退 Java executor。“生命回满”已从 `hpChange = 9999` 哨兵值改为显式 `fullHeal` 字段，并由前端事件页和后端 CLI 共用；翅膀雕像摧毁、黏液世界放手、村庄板烧鸡腿堡已从 Java 随机分支迁到 JSON 随机范围。后端 CLI `selftest` 会扫描所有 JSON 固定/随机事件结果，确认字段可解析、随机范围顺序有效、`relicId` 可创建、`fullHeal` 不与普通 `hpChange` 混用，并确认没有固定结果的选项都已标记 Java executor。`relic_nuke` 和 `relic_five_cards` 已补为特殊藏品占位，避免事件奖励引用空 ID。投资、随机藏品、命运抉择等复杂逻辑仍保留 Java executor。测试覆盖普通事件文本来自 JSON、固定结果来自 JSON、随机范围结果来自 JSON、固定字段解析、显式回满、固定结果藏品 ID、随机选项回退 Java，以及命运抉择条件仍按玩家藏品判断。剩余工作：继续把可数据化的固定/简单随机事件结果迁入 JSON，并为随机/特殊事件设计更明确的 executor 分层。

### 位置

- 包：`com.fabricatedbook.core.event`
- 类：`EventHandler`
  - `EventResultResolver`
- 方法：
  - `getEventNames()`
  - `getEventDescription(String eventName)`
  - `getOptions(String eventName, Player player)`
  - `executeEvent(String eventName, int optionIndex, Player player)`
- 相关资源：
  - `core/src/main/resources/data/events.json`
- 相关前端：
  - `EventScreen`

### 问题

事件标题、描述、选项和结果主要写在 `EventHandler`。`events.json` 存在，但还没有成为运行时核心规则来源。

这使内容迁移、文案维护、事件条件展示、事件测试都比较重。复杂事件需要 Java 代码支持是合理的，但所有事件都硬编码会让内容和规则难以分层。

### 期望实现

短期可以保留 Java handler，但需要明确边界：

- 普通事件的标题、描述、选项、固定数值来自 JSON。
- 特殊事件和复杂条件由 Java handler 执行，并在 JSON 选项上用 `executor: "java"` 标明边界。
- `EventHandler` 负责调度，不负责塞满所有文本和所有规则。
- `EventResultResolver` 或后续 executor 负责把数据选项转换为可执行结果。

### 解决方式

1. 对照 `events.json` 和 `EventHandler` 建立事件字段差异清单。
2. 先把纯文本和固定选项迁到 JSON。
3. 为事件结果设计轻量 DSL，例如金币、生命、回满、藏品、卡牌、结局 outcome。
4. 把固定 JSON 结果转换从 `EventHandler` 抽到 resolver。（已完成：`EventResultResolver`）
5. 特殊事件保留 Java executor。
6. 补事件数据自检和关键事件单元测试。

### 影响面

- 影响 `EventScreen` 显示、命运抉择、隐藏结局路线、事件奖励。
- 如果事件结果涉及存档或地图推进，需要与 `GameRunState`、`MapScreen` 的节点完成时机同步。
- 这是内容系统重构，建议在地图/节点规则下沉后再做。

## 验证建议

每完成一个后端问题，应至少运行：

```bash
./gradlew test
printf 'selftest\nseedtest 12345\nsavetest\nquit\n' | ./gradlew runBackendDebug --args="--seed=12345"
```

涉及地图规则时，额外补充：

- 同 seed 地图结构稳定。
- 不同 seed 地图通常不同。
- 第 4 层迷雾 Boss 后命运抉择结构稳定。
- CLI 与前端使用同一地图模型。

涉及存档字段时，额外补充：

- 旧存档缺字段可正常读取。
- 新字段能保存和恢复。
- 战斗中退出仍回到战斗前快照。
