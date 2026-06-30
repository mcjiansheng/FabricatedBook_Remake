# 项目文档导航

本文档按阅读目的整理仓库内现有文档，方便后续开发、调试、内容迁移和 Agent 协作时快速定位资料。更新时间：2026-06-18。

## 推荐阅读顺序

### 第一次了解项目

1. [../README.md](../README.md) - 项目定位、整体架构、核心系统、构建与运行入口。
2. [game_encyclopedia/README.md](game_encyclopedia/README.md) - 游戏百科总入口，按规则、卡牌、藏品、怪物、节点事件和结局拆分。
3. [frontend_design.md](frontend_design.md) - 当前 Java + LibGDX 前端界面规格和交互流程。
4. [backend_cli_debug.md](backend_cli_debug.md) - 不启动前端时验证后端地图、事件、战斗和存档的命令行入口。

### 准备修改代码或数据

1. [../AGENTS.md](../AGENTS.md) - AI Agent 项目规约，复杂功能必须先明确需求、规划、复用架构并验证。
2. [../CONTRIBUTING.md](../CONTRIBUTING.md) - GitHub 协作、分支、提交、PR、Review 和 Agent 协作规范。
3. [../TODO.md](../TODO.md) - 当前阶段完成情况和剩余重点。
4. [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md) - 原版内容迁移、JSON DSL、代码接入点和分阶段验收清单。

### 准备做 UI 或交互

1. [frontend_design.md](frontend_design.md) - 当前前端规格，以 `1280 x 720` 和 LibGDX Scene2D 为准。
2. [original_frontend_design_and_comparison.md](original_frontend_design_and_comparison.md) - 原版 C + SDL2 前端结构，以及与重制版的差异。
3. [frontend_debug_cli.md](frontend_debug_cli.md) - 通过命令行直接进入标题、字体、地图、战斗、商店、事件等调试界面。
4. [computer_use_app_capture.md](computer_use_app_capture.md) - macOS 上打包并用 Computer Use 抓取/操作 LibGDX 调试窗口。
5. [frontend_ui_acceptance.md](frontend_ui_acceptance.md) - P1 前端状态、窗口边界和截图验收矩阵。

### 准备做地图系统

1. [game_encyclopedia/overview.md](game_encyclopedia/overview.md) 和 [game_encyclopedia/nodes_events.md](game_encyclopedia/nodes_events.md) - 玩法层面的地图、节点、环境效果和事件规则。
2. [original_map_analysis.md](original_map_analysis.md) - 原版 C 项目的列优先稀疏地图、节点概率、滑动窗口连接和环境效果分析。
3. [map_test_report.md](map_test_report.md) - 后端 CLI 地图专项测试报告，记录过首尾行硬编码、DECISION 处理、迷雾后抉择等问题。
4. [layer_by_layer_test.md](layer_by_layer_test.md) - 第 1 层逐层地图测试记录，是地图专项报告的早期简版。

### 准备做怪物或敌人行为

1. [game_encyclopedia/monsters.md](game_encyclopedia/monsters.md) - 各层敌人、Boss 和隐藏 Boss 图鉴。
2. [enemy_action_dsl.md](enemy_action_dsl.md) - 怪物 `actionScript` 兼容格式、推荐写法、数据自检和图片映射规范。
3. [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md) - 内容迁移时涉及的怪物 JSON 字段和历史迁移说明。

### 准备做随机种子、存档或回归验证

1. [backend_regression_checklist.md](backend_regression_checklist.md) - 后端提交前最小回归命令、通过标志、失败处理和专项加跑清单。
2. [run_seed_and_save.md](run_seed_and_save.md) - 对局种子、随机流 key、战斗中退出规则、单槽位存档和 CLI 验证方式。
3. [backend_cli_debug.md](backend_cli_debug.md) - `runBackendDebug` 命令、地图/战斗命令、自检、`seedtest`、`savetest`、`flowtest` 和 `routetest`。
4. [debug_test_report.md](debug_test_report.md) - 2026-05-31 后端 CLI 五层通关测试报告和历史 Bug 记录。

## 文档分类

### 项目总览与协作规范

| 文档 | 作用 | 适合什么时候读 |
|:--|:--|:--|
| [../README.md](../README.md) | 项目概述、架构分层、核心接口、系统设计、构建命令 | 新人入门、评估改动影响面 |
| [../AGENTS.md](../AGENTS.md) | AI Agent 工作规约，强调需求澄清、规划、复用架构和验证 | 任何 Agent 修改代码、数据或文档前 |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | GitHub 协作、分支、提交、PR、Review、安全和 Agent 规范 | 提交、开 PR、多人协作前 |
| [../TODO.md](../TODO.md) | 阶段完成情况、集成测试缺口、当前剩余重点 | 排期、挑任务、确认未完成事项 |

### 玩法与内容资料

| 文档 | 作用 | 适合什么时候读 |
|:--|:--|:--|
| [game_encyclopedia/README.md](game_encyclopedia/README.md) | 游戏百科总入口，专题拆分规则、地图、节点、卡牌、Buff、药水、藏品、敌人、事件、结局 | 查玩法设计、核对内容和文案 |
| [game_encyclopedia/cards.md](game_encyclopedia/cards.md) | 卡牌池和卡牌类型说明 | 补卡牌、改卡牌文案或核对卡牌设计 |
| [game_encyclopedia/relics.md](game_encyclopedia/relics.md) | 各稀有度藏品、特殊藏品、负面藏品 | 补藏品、核对藏品效果和隐藏路线相关条件 |
| [game_encyclopedia/monsters.md](game_encyclopedia/monsters.md) | 各层敌人、Boss 和隐藏 Boss 图鉴 | 补怪物、改行动或核对 Boss 解锁条件 |
| [game_encyclopedia/nodes_events.md](game_encyclopedia/nodes_events.md) | 节点系统、事件列表、命运抉择 | 修改节点、事件、商店、命运抉择时 |
| [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md) | 原版内容迁移指南，说明 JSON 字段、支持的 effect/action DSL、迁移顺序和验收 | 补卡牌、怪物、藏品、药水、事件或地图数据 |
| [enemy_action_dsl.md](enemy_action_dsl.md) | 怪物 actionScript 运行时入口、推荐 action 命名、resolver 自检和图片映射规范 | 新增怪物、改敌人行动或接入敌人立绘时 |

### 前端设计与调试

| 文档 | 作用 | 适合什么时候读 |
|:--|:--|:--|
| [frontend_design.md](frontend_design.md) | 当前前端规格，覆盖标题、选角、地图、战斗、事件、商店、总览和待完善点 | 修改 UI、交互、布局、视觉资源前 |
| [original_frontend_design_and_comparison.md](original_frontend_design_and_comparison.md) | 原版前端源码阅读结果，以及原版与重制版差异和还原建议 | 需要贴近原版体验或判断设计取舍时 |
| [frontend_debug_cli.md](frontend_debug_cli.md) | 前端命令行入口和可用调试 screen | 快速打开特定界面复现 UI 问题 |
| [computer_use_app_capture.md](computer_use_app_capture.md) | Computer Use 调试 app 打包、启动和抓取流程 | 需要屏幕控制、截图或自动化点按 LibGDX 窗口时 |
| [frontend_ui_acceptance.md](frontend_ui_acceptance.md) | 前端状态矩阵、通过标准与截图记录约定 | 验收 UI 改动或补复杂调试状态时 |

### 后端调试、种子和存档

| 文档 | 作用 | 适合什么时候读 |
|:--|:--|:--|
| [backend_regression_checklist.md](backend_regression_checklist.md) | 后端最小回归测试矩阵、提交前命令、通过标志和失败处理 | 后端提交前、修复失败测试后 |
| [backend_cli_debug.md](backend_cli_debug.md) | 后端命令行调试系统说明，覆盖地图、事件、战斗、自检和脚本化回归 | 验证核心逻辑，不想启动前端时 |
| [run_seed_and_save.md](run_seed_and_save.md) | 对局种子、随机流隔离、存档结构、战斗中退出规则和 CLI 验证 | 改随机、奖励、商店、存档、继续游戏时 |

### 原版分析与重构依据

| 文档 | 作用 | 适合什么时候读 |
|:--|:--|:--|
| [original_map_analysis.md](original_map_analysis.md) | 原版 C 地图生成机制分析，重点是稀疏列图和滑动窗口连接 | 重构地图结构、连接算法或环境效果时 |
| [original_frontend_design_and_comparison.md](original_frontend_design_and_comparison.md) | 原版 C 前端 UI/交互与重制版对比 | 还原原版前端或补差异时 |

### 测试报告与历史问题

| 文档 | 作用 | 当前状态 |
|:--|:--|:--|
| [debug_test_report.md](debug_test_report.md) | 后端 CLI 五层通关测试报告，记录 deck、敌人意图、奖励回复、手牌索引等历史问题 | 作为回归和历史问题参考 |
| [map_test_report.md](map_test_report.md) | 地图专项测试报告，记录首尾行硬编码、DECISION 处理、迷雾后抉择等问题 | 作为地图修复和验收参考 |
| [layer_by_layer_test.md](layer_by_layer_test.md) | 第 1 层地图测试记录 | 可视为 `map_test_report.md` 的早期补充 |

## 数据文件索引

这些文件不是说明文档，但属于后续内容阅读和迁移时必须核对的数据源。

| 数据文件 | 内容 |
|:--|:--|
| [../core/src/main/resources/data/cards/warrior.json](../core/src/main/resources/data/cards/warrior.json) | 战士卡牌数据，当前约 32 张 |
| [../core/src/main/resources/data/monsters/level1.json](../core/src/main/resources/data/monsters/level1.json) | 第 1 层怪物组 |
| [../core/src/main/resources/data/monsters/level2.json](../core/src/main/resources/data/monsters/level2.json) | 第 2 层怪物组 |
| [../core/src/main/resources/data/monsters/level3.json](../core/src/main/resources/data/monsters/level3.json) | 第 3 层怪物组 |
| [../core/src/main/resources/data/monsters/level4.json](../core/src/main/resources/data/monsters/level4.json) | 第 4 层怪物组 |
| [../core/src/main/resources/data/monsters/level5.json](../core/src/main/resources/data/monsters/level5.json) | 第 5 层怪物组 |
| [../core/src/main/resources/data/relics.json](../core/src/main/resources/data/relics.json) | 45 个藏品数据 |
| [../core/src/main/resources/data/potions.json](../core/src/main/resources/data/potions.json) | 10 个药水数据 |
| [../core/src/main/resources/data/events.json](../core/src/main/resources/data/events.json) | 8 个事件数据 |
| [../core/src/main/resources/data/maps/levels.json](../core/src/main/resources/data/maps/levels.json) | 地图层级配置 |

## 快速任务索引

| 我要做的事 | 优先阅读 |
|:--|:--|
| 新增或修改复杂玩法规则 | [../AGENTS.md](../AGENTS.md), [../README.md](../README.md), [game_encyclopedia/README.md](game_encyclopedia/README.md) |
| 修卡牌效果或预览 | [game_encyclopedia/cards.md](game_encyclopedia/cards.md), [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md), [frontend_design.md](frontend_design.md) |
| 修怪物行动或意图 | [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md), [backend_cli_debug.md](backend_cli_debug.md), [debug_test_report.md](debug_test_report.md) |
| 修地图生成 | [original_map_analysis.md](original_map_analysis.md), [map_test_report.md](map_test_report.md), [run_seed_and_save.md](run_seed_and_save.md) |
| 修商店/奖励/药水 | [frontend_design.md](frontend_design.md), [game_encyclopedia/nodes_events.md](game_encyclopedia/nodes_events.md), [game_encyclopedia/effects_potions.md](game_encyclopedia/effects_potions.md), [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md) |
| 验证后端主链路 | [backend_cli_debug.md](backend_cli_debug.md), [run_seed_and_save.md](run_seed_and_save.md) |
| 验证前端界面 | [frontend_debug_cli.md](frontend_debug_cli.md), [computer_use_app_capture.md](computer_use_app_capture.md) |
| 准备提交或 PR | [../CONTRIBUTING.md](../CONTRIBUTING.md), [../AGENTS.md](../AGENTS.md) |

## 维护建议

- 新增设计说明、调试流程或测试报告后，请同步补到本文档对应分类。
- 如果某份测试报告中的 Bug 已修复，建议在原报告中追加“修复状态”或新建后续报告，避免历史问题与当前状态混淆。
- 如果数据格式或 DSL 变化，请优先更新 [ai_original_content_migration_guide.md](ai_original_content_migration_guide.md)，再更新本文档索引。
- 如果前端行为变化，请同步更新 [frontend_design.md](frontend_design.md) 和对应调试入口说明。
