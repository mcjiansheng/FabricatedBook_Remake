# 后端命令行调试系统说明

本文档说明 `BackendDebugLauncher` 的用途、启动方式、命令列表和内部机制。该调试系统用于绕过 LibGDX 前端，直接验证后端的地图生成、路线选择、事件处理和战斗交互逻辑。

## 一、设计目标

后端命令行调试系统解决两个问题：

- 前端仍在迭代时，可以独立验证 `core` 模块的规则和数据流。
- 战斗、地图、事件等逻辑可以用命令行脚本快速回归，不需要手动点击 UI。

入口类位于：

```text
desktop/src/main/java/com/fabricatedbook/desktop/BackendDebugLauncher.java
```

Gradle 任务位于：

```text
desktop/build.gradle
```

任务名：

```text
runBackendDebug
```

## 二、启动方式

macOS / Linux：

```bash
./gradlew runBackendDebug
```

Windows：

```bat
gradlew.bat runBackendDebug
```

启动后会创建一个调试用战士玩家，初始状态如下：

```text
玩家 ID: debug-player
玩家名称: 调试战士
职业: WARRIOR
初始 HP: 80/80
初始金币: 80
初始层级: 荒野
```

随后系统会生成当前层地图，并进入地图命令循环。

## 三、地图调试命令

地图命令在提示符 `>` 后输入。

| 命令 | 作用 |
|:--|:--|
| `help` | 显示地图命令和战斗入口说明 |
| `status` | 查看玩家 HP、金币、当前层 |
| `map` | 打印当前层地图和可选路线 |
| `routes` | 只打印当前位置可选路线 |
| `choose <编号>` | 选择一条可达路线并进入对应节点 |
| `go <编号>` | `choose` 的别名 |
| `battle` | 不走地图，直接启动一场普通战斗 |
| `deck` | 查看抽牌堆、手牌、弃牌堆数量 |
| `newmap` | 重新生成当前层地图 |
| `quit` / `exit` | 退出调试控制台 |

地图输出示例：

```text
地图: 荒野 (4x3) 环境: 无
[ 0,0 战斗  ] [ 0,1 战斗  ] [@0,2 战斗  ] [ 0,3 战斗  ]
[ 1,0 战斗  ] [!1,1 事件  ] [!1,2 战斗  ] [!1,3 战斗  ]
[ 2,0 战斗  ] [ 2,1 战斗  ] [ 2,2 抉择  ] [ 2,3 战斗  ]
可选路线:
  1. 不期而遇 (1,1)
  2. 战斗 (1,2)
  3. 战斗 (1,3)
```

地图标记含义：

| 标记 | 含义 |
|:--|:--|
| `@` | 玩家当前位置 |
| `!` | 当前可选择的下一步节点 |
| `*` | 已访问节点 |
| 空格 | 不可直接到达或未访问节点 |

## 四、节点处理逻辑

执行 `choose <编号>` 后，调试器调用 `MapGraph.moveTo(node)` 移动玩家位置，并根据节点类型进入对应后端流程。

| 节点类型 | 命令行处理 |
|:--|:--|
| `FIGHT` | 启动普通战斗 |
| `EMERGENCY` | 启动精英战斗 |
| `BOSS` | 启动 Boss 战斗 |
| `UNEXPECTED` | 随机抽取一个 `EventHandler` 事件并显示选项 |
| `DECISION` | 按事件节点处理 |
| `SHOP` | 当前为轻量占位处理，获得 15 金币 |
| `REWARD` | 当前为轻量占位处理，获得 25 金币 |
| `SAFEHOUSE` | 回复 12 点生命值 |

当玩家到达当前层终点时，调试器会自动进入下一层地图。到达第五层终点后，路线调试结束。

## 五、事件调试流程

事件由 `EventHandler` 驱动。进入事件节点后，调试器会：

1. 从 `EventHandler.getEventNames()` 随机选择一个事件。
2. 使用 `EventHandler.getOptions(eventName)` 打印选项。
3. 等待输入选项编号。
4. 调用 `EventHandler.executeEvent(eventName, optionIndex)`。
5. 将 `EventResult` 中的金币和生命变化应用到玩家。

示例：

```text
事件: 投资
  1. 0 金币 - 投资 0 金币
  2. 50 金币 - 投资 50 金币
  3. 100 金币 - 投资 100 金币
输入事件选项编号，或回车选择 1。
event> 2
```

如果输入为空，默认选择第 1 项。如果输入超出范围，会被限制在有效选项范围内。

## 六、战斗调试命令

进入战斗后，提示符会变为 `battle>`。

| 命令 | 作用 |
|:--|:--|
| `help` | 显示战斗命令 |
| `state` | 查看玩家、敌人、手牌状态 |
| `hand` | 查看当前手牌 |
| `enemies` | 查看敌人 HP、格挡和意图 |
| `play <牌编号> [敌人编号]` | 使用指定手牌，敌人编号可省略，默认第 1 个存活敌人 |
| `end` | 结束当前回合，让敌人行动并进入下一回合 |
| `auto` | 自动打完整场战斗 |
| `quit` | 退出整个调试控制台 |

战斗状态示例：

```text
玩家 HP 80/80 格挡 0 能量 3 回合 1
敌人:
  1. 命令行假人 HP 36/36 格挡 0 意图 ATTACK
手牌:
  1. [1] 攻击 - 造成 6 点伤害
  2. [1] 防御 - 获得 6 点格挡
```

手动出牌示例：

```text
battle> play 1
battle> play 2
battle> end
```

攻击牌默认以第 1 个存活敌人为目标；如果存在多个敌人，可以显式指定：

```text
battle> play 3 2
```

表示打出第 3 张手牌，目标为第 2 个存活敌人。

## 七、战斗内部接线

命令行战斗复用正式后端战斗系统：

```text
BackendDebugLauncher
  -> CombatEngine
  -> ActionManager
  -> CombatAction
  -> DamageCalculator
  -> BuffHook
```

战斗初始化流程：

1. 根据节点类型创建敌人列表。
2. 创建 `CombatEngine`。
3. 为玩家创建 `RelicManager` 并注入 `CombatEngine`。
4. 设置 `ConsoleNotifier`，把战斗事件打印到命令行。
5. 调用 `CombatEngine.initBattle(player, enemies)`。

`ConsoleNotifier` 实现 `ViewNotifier`，负责将下列事件输出到终端：

- 战斗开始
- 动作执行
- 出牌
- 回合开始
- 战斗结束

这意味着命令行调试器不会复制战斗逻辑，而是直接使用正式后端结算路径。

## 八、敌人配置

当前命令行调试器使用轻量测试敌人，便于稳定验证流程：

| 节点类型 | 敌人 | HP | 行动脚本 |
|:--|:--|--:|:--|
| `FIGHT` | 命令行假人 | 36 | `atk1` |
| `EMERGENCY` | 命令行精英 | 48 | `atk7`, `atk5x2`, `def10` |
| `BOSS` | 命令行首领 | 70 | `atk8`, `def8`, `atk12` |

后续如果要调试 JSON 怪物数据，可以把 `createEnemies(NodeType nodeType)` 改为从 `DataLoader.loadMonsters(level)` 里选择怪物组。

## 九、脚本化回归示例

调试器支持标准输入，因此可以用管道执行简单回归。

查看状态、地图，然后退出：

```bash
printf 'status\nmap\nquit\n' | ./gradlew runBackendDebug
```

启动一场普通战斗并自动打完：

```bash
printf 'battle\nauto\nquit\n' | ./gradlew runBackendDebug
```

选择第一条路线。如果第一条路线进入事件，随后选择第 1 个事件选项：

```bash
printf 'choose 1\n1\nquit\n' | ./gradlew runBackendDebug
```

注意：地图是随机生成的，`choose 1` 可能进入战斗、事件、奖励等不同节点。写自动化脚本时建议优先使用 `battle` 命令验证战斗，使用 `map` / `routes` 人工观察路线。

## 十、跨平台说明

该命令行调试系统不依赖 LibGDX 窗口，不需要 OpenGL，也不需要 macOS 的 `-XstartOnFirstThread` 参数。

跨平台入口：

| 系统 | 命令 |
|:--|:--|
| macOS | `./gradlew runBackendDebug` |
| Linux | `./gradlew runBackendDebug` |
| Windows CMD / PowerShell | `gradlew.bat runBackendDebug` |

`runGame` 是 LibGDX 桌面前端启动任务，在 macOS 上会自动附加 `-XstartOnFirstThread`。`runBackendDebug` 是纯命令行任务，不附加平台特定 JVM 参数。

## 十一、当前边界

命令行调试器的目标是调通后端机制，不是完整替代游戏前端。当前有以下边界：

- 商店节点目前是占位处理，只给玩家增加少量金币，没有进入完整购买界面。
- 奖励节点目前是占位处理，只给玩家增加金币，没有选择卡牌或藏品。
- 事件结果目前只应用金币和生命变化，`relicId` 暂未实例化为真实藏品。
- 地图和敌人调试数据采用轻量固定配置，尚未完全接入 JSON 怪物池。
- `auto` 战斗用于流程回归，不代表真实 AI 或最佳出牌策略。

这些边界不会影响核心后端链路的调试：地图生成、路线选择、事件选项、战斗初始化、出牌、Action 队列、Buff 修正、敌人回合和胜负结算都已经通过正式后端路径执行。

## 十二、建议扩展方向

后续可以按以下顺序增强：

1. 让战斗敌人从 `DataLoader.loadMonsters(level)` 中抽取。
2. 给 `runBackendDebug` 增加参数，如 `--level 3`、`--seed 1234`、`--battle boss`。
3. 将商店节点接入 `ShopManager` 的购买流程。
4. 将奖励节点接入卡牌奖励和藏品奖励。
5. 将事件的 `relicId` 结果接入 `RelicManager`。
6. 增加 JUnit 测试覆盖 `MapGraph` 路线可达性和 `CombatEngine` 基础战斗流。
