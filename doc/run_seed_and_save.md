# 对局种子与对局内存档说明

本文档说明当前项目的对局随机种子、对局内自动存档、继续游戏，以及后端 CLI 调试验证方式。

## 1. 功能目标

本功能对齐类《杀戮尖塔》的对局语义：

- 新游戏开始时生成一个对局随机种子。
- 地图、敌人选择、战斗洗牌、奖励、商店等随机结果从该种子派生。
- 玩家退出游戏后可以从标题页选择“继续游戏”恢复对局。
- 存档记录战斗外长期状态：路径、层级、角色状态、金币、卡牌、藏品、药水和已领取奖励。
- 存档不记录战斗过程。战斗中退出后，读档回到尚未进入该战斗节点的状态。
- 同一战斗节点重新进入时，战斗随机流相同，因此起手抽牌和后续洗牌顺序可复现。

## 2. 核心对象

核心对局状态位于：

```text
core/src/main/java/com/fabricatedbook/core/run/GameRunState.java
```

`GameRunState` 保存：

- `seed`：本次对局种子。
- `player`：玩家长期状态。
- `currentLayerIdx`：当前层索引。
- `completedNode`：最近完成并记录到路径的节点。
- `activeNode`：已经进入但尚未完成的节点，主要用于战斗中退出。
- `combatBaseline`：进入战斗前的玩家快照。

随机数通过 `randomFor(String key)` 创建。不同系统使用不同 key 派生独立随机流，例如：

```text
map-layout
combat:<layer>:<col>:<row>:<type>
enemies:<layer>:<col>:<row>:<type>
reward:<layer>:<col>:<row>
shop:<layer>:<col>:<row>:<type>
```

这种设计避免保存 `java.util.Random` 的内部状态，同时能保证同一对局种子和同一事件 key 得到稳定结果。`GameRunStateRandomTest` 会固定同 seed/key 序列稳定、不同 key 隔离，以及某个随机流的消耗不会推进另一个随机流。

## 2.1 随机数耦合规避

早期实现已经用 key 将地图、敌人、事件、战斗和奖励分开，但部分子系统内部仍可能共享同一个 `Random`。共享随机流会产生“随机数耦合”：前一个事件是否额外消耗随机数，会改变后一个事件结果。

当前规避原则：

- 高层系统不共享随机流。地图生成、选怪、事件名称、事件结果、商店、战斗、奖励分别使用不同 key。
- 同一系统内的独立结果也尽量拆流。例如战斗奖励使用：
  - `reward:<node>:relic-chance`
  - `reward:<node>:relic-pick`
  - `reward:<node>:potion-chance`
  - `reward:<node>:potion-pick`
  - `reward:<node>:card-pick`
- 地图节点类型按坐标派生随机流，某个格点或连接的随机消耗不会影响另一个格点。
- 商店卡牌池、藏品池、药水池和各商品价格分别派生随机流。
- 战斗过程随机与胜利金币/回血奖励拆开，玩家打牌过程中的随机触发不会影响胜利奖励掷点。

因此，本项目不会出现类似“第一只怪物是谁影响问号节点是否出怪”的跨系统耦合：第一只怪物选择使用敌人节点 key，问号事件/结果使用事件节点 key，两者互不消耗同一随机序列。

当前仍允许共享的范围是“同一战斗过程内部”的动态随机，例如卡牌随机效果、敌人随机行为、敌人被动触发等。这些属于同一场战斗模拟的一部分，不会影响地图、事件、商店或战斗胜利奖励。

## 3. 存档结构

存档管理器位于：

```text
core/src/main/java/com/fabricatedbook/data/SaveManager.java
```

当前保存文件为：

```text
saves/save.json
```

运行目录不同会影响实际位置：

- 前端 `runGame` 使用 `core/src/main/resources` 作为工作目录。
- 后端 `runBackendDebug` 默认使用对应 Gradle 任务的工作目录。

`SaveManager.saveRun(GameRunState)` 写入版本 2 对局存档，包含：

- 对局种子。
- 当前层。
- 已完成节点。
- 战斗中活跃节点。
- 战斗前玩家快照。
- 玩家生命、金币、卡牌、藏品、药水等长期状态。

`SaveManager.loadRun()` 会恢复 `GameRunState`。如果存档是在战斗中写入，读取时不会恢复战斗过程，而是使用战斗前快照并清理 `activeNode`，让玩家回到战斗节点未完成的状态。

## 4. 战斗中退出规则

进入战斗节点时：

1. 地图层调用 `runState.beginCombat(nodeRef)`。
2. `GameRunState` 记录 `activeNode` 和 `combatBaseline`。
3. 自动保存时，`SaveManager` 保存的是 `combatBaseline`，不是战斗中的玩家状态。

战斗胜利并领取奖励时：

1. 奖励实际写入玩家长期状态。
2. 调用 `runState.completeActiveNode()`。
3. 自动保存此时记录奖励和路径进度。

因此：

- 战斗中掉线、关闭窗口、ESC 保存退出：读档后视为尚未打这场战斗。
- 战斗胜利后奖励已领取再退出：读档后奖励仍保留，节点已完成。
- 最终 Boss 后进入结局前会先提交 active combat；`EndingScreen` 显示时会删除当前 run 存档，结局画面关闭后不会留下可继续存档。

非战斗节点使用同一套 active node 机制：

1. 刚进入商店、事件或安全屋时，`GameRunState` 先记录节点入口前快照。
2. 如果尚未做出不可撤销操作，自动保存仍写入入口前快照；读档后视为尚未完成该节点。
3. 事件选择、商店购买/删牌、安全屋结算等操作会标记 active node progress committed。
4. 已提交非战斗进度自动保存时，`SaveManager` 保存当前玩家长期状态，并把该 active 节点写为 completed。

因此：

- 只进入非战斗节点后退出：读档后回到节点入口前，不能吃到入口副作用。
- 做出事件选择、购买商品或完成安全屋操作后退出：读档后保留结果，节点视为已完成，避免重复领取或重复购买。

## 5. 随机源接入范围

已接入外部随机源或 key 派生随机源的后端组件包括：

- `CombatEngine`：战斗内随机、金币奖励、战士胜利回血、初始洗牌。
- `AbstractEntity.shuffleDiscardToDraw()`：弃牌堆洗回抽牌堆。
- `CardPool.randomSelect(...)`：奖励/商店卡牌选择。
- `RelicFactory.randomRelic(...)`：随机藏品奖励。
- `ShopManager`：商品生成与价格波动。
- `LayerMapConfig` / `LayerMapGraph`：后端 CLI 与前端地图共用的稀疏层地图生成。
- `EventHandler`：事件结果中的随机金币、回血和投资掷点。

前端 `MapScreen` 使用 `GameRunState.randomFor(...)` 为地图、选怪、事件、商店和战斗创建随机源。

## 6. 后端 CLI 调试

CLI 入口：

```bash
./gradlew runBackendDebug
```

指定种子启动：

```bash
./gradlew runBackendDebug --args="--seed=12345"
```

新增命令：

| 命令 | 作用 |
|:--|:--|
| `seed` | 打印当前对局种子 |
| `save` | 保存当前对局 |
| `load` / `continue` | 从 `saves/save.json` 继续对局 |
| `newrun [seed]` | 使用指定或随机种子开始新对局 |
| `seedtest [seed]` | 验证同种子地图和战斗起手抽牌可复现 |
| `savetest` | 验证战斗中存档回到战斗前快照 |
| `flowtest` | 验证事件、奖励节点、商店、安全屋和药水丢弃等非战斗节点提交后保存语义 |
| `routetest` | 验证隐藏路线、门扉隐藏选项、第 5 层隐藏 Boss 和回头结局条件 |

推荐回归命令：

```bash
printf 'selftest\nseedtest 12345\nsavetest\nflowtest\nroutetest\nquit\n' \
  | ./gradlew runBackendDebug --args="--seed=12345"
```

期望输出包含：

```text
SELFTEST PASS
SEEDTEST PASS
SAVETEST PASS
FLOWTEST PASS
ROUTETEST PASS
```

手动保存/读档回归：

```bash
printf 'save\nload\nseed\nstatus\nquit\n' \
  | ./gradlew runBackendDebug --args="--seed=777"
```

读档后应保持同一个种子、玩家状态和地图布局。

## 7. 当前边界

- 存档当前为单槽位 `saves/save.json`。
- `GameRunState.randomFor(key)` 是按 key 派生随机流，不保存随机调用次数；应为独立随机事件使用独立 key，避免把无关事件放进同一个随机流。
- 后端 CLI 与前端地图已共用 `LayerMapGraph` 生成节点和连接，节点进入副作用共用 `NodeEntryResolver`，敌人遭遇选择共用 `EnemyEncounterResolver`；`MapScreen` 仍保留渲染用节点包装。
- CLI 的 `savetest` 会写入测试存档；测试后如不需要该存档，可以删除对应运行目录下的 `saves/save.json`。
