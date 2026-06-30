# 后端最小回归测试执行清单

> 适用范围：后端 core 规则、数据 JSON、存档、后端 CLI、以及会影响地图/事件/商店/战斗主链路的提交。

## 每次后端提交前

按顺序运行：

```bash
./gradlew test
git diff --check
printf 'selftest\nseedtest 12345\nsavetest\nflowtest\nquit\n' | ./gradlew runBackendDebug --args="--seed=12345"
```

期望结果：

- `./gradlew test` 输出 `BUILD SUCCESSFUL`。
- `git diff --check` 没有输出。
- 后端 CLI 输出 `SELFTEST PASS`、`SEEDTEST PASS`、`SAVETEST PASS`、`FLOWTEST PASS`。

失败处理：

- 任一命令失败时，不提交。
- 先阅读第一处失败断言或异常，确认是实现问题、测试期望过期，还是数据文件破坏。
- 如果修改了行为语义，同步更新相关单元测试、CLI 自检和 TODO 文档。
- 如果只是环境或 Gradle 缓存问题，应重新运行同一命令确认；仍失败时在提交说明或任务记录中写明未通过项和原因。

## 覆盖矩阵

| 范围 | 必跑命令 | 通过标志 | 主要覆盖 |
|:--|:--|:--|:--|
| core 规则和数据 | `./gradlew test` | `BUILD SUCCESSFUL` | 战斗、卡牌 DSL、药水、藏品、事件、地图、商店、存档 |
| 格式和补丁质量 | `git diff --check` | 无输出 | 尾随空白、补丁格式 |
| 数据/注册表自检 | CLI `selftest` | `SELFTEST PASS` | 卡牌 JSON/CardPool、effect handler、事件结果、藏品/药水/怪物加载、商店商品 |
| 随机可复现 | CLI `seedtest 12345` | `SEEDTEST PASS` | 同 seed 地图和战斗起手稳定，不同 seed 通常不同 |
| 存档语义 | CLI `savetest` | `SAVETEST PASS` | 战斗/非战斗节点中途保存、已提交非战斗节点保存 |
| 节点流程 | CLI `flowtest` | `FLOWTEST PASS` | 事件、奖励节点、商店购买/删牌、安全屋、药水丢弃提交后保存 |

## 变更专项加跑

- 改地图生成、层配置、节点类型：加看 `LayerMapGraphTest`，并运行 `seedtest 12345`。
- 改商店购买、删牌、价格或商品池：加看 `ShopManagerTest`，并运行 `flowtest`。
- 改存档字段或节点提交时机：加看 `GameRunStateSaveTest`，并运行 `savetest` 和 `flowtest`。
- 改藏品拾取、战斗 hook 或奖励展开：加看 `DataRelicTest`、`CombatPreviewCalculatorTest`、`EventHandlerTest` 中相关用例。
- 改卡牌 JSON 或 effect DSL：加看 `CardEffectParserTest`、`CombatPreviewCalculatorTest`，并运行 `selftest`。
- 改事件 JSON 或命运抉择：加看 `EventHandlerTest`，并运行 `selftest` 和 `flowtest`。

## 记录要求

提交或任务总结中至少写明：

- 本次改动范围。
- 已运行的命令。
- 关键通过标志。
- 未运行的验证及原因。

如果某项失败后被修复，应在最终总结里只保留修复后的通过结果；如果仍未通过，不应把该子修复标为完成。
