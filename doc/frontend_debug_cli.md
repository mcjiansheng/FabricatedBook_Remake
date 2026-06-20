# 前端命令行调试入口

桌面端启动器支持通过命令行直接进入指定前端界面，用于快速复现 UI、字体、地图、战斗、商店和事件状态。

## 启动方式

在项目根目录执行：

```bash
./gradlew desktop:runGame -Pargs="font"
```

也可以使用 Gradle 的 JavaExec 参数：

```bash
./gradlew desktop:runGame --args="font"
```

## Computer Use 调试 app

macOS 上直接用 `desktop:runGame` 启动时，LWJGL 窗口会作为裸 `java`
进程暴露，Computer Use 可能无法按应用名识别。需要屏幕控制调试时，先生成并安装
用户级调试 app：

```bash
./gradlew desktop:installComputerUseApp
```

安装位置：

```text
~/Applications/FabricatedBookDebug.app
```

启动标题界面：

```bash
./gradlew desktop:openComputerUseApp -Pargs="title"
```

启动字体调试界面：

```bash
./gradlew desktop:openComputerUseApp -Pargs="font"
```

启动后，Computer Use 使用下面任一目标抓取窗口：

```text
FabricatedBookDebug
/Users/mcjiansheng/Applications/FabricatedBookDebug.app
```

如果同时保留了 `desktop/build/computerUseApp/image/FabricatedBookDebug.app`，
bundle id `com.fabricatedbook.debug` 可能有歧义；优先使用应用名或用户应用目录的完整路径。

面向后续 Codex 或其它屏幕控制 agent 的完整抓取流程见：
`doc/computer_use_app_capture.md`。

## 可用入口

- 标题界面：
  `./gradlew desktop:runGame -Pargs="title"`

- 角色选择界面：
  `./gradlew desktop:runGame -Pargs="character-select"`

- 字体调试界面：
  `./gradlew desktop:runGame -Pargs="font"`

- 第 1 层地图：
  `./gradlew desktop:runGame -Pargs="map"`

- 指定层地图：
  `./gradlew desktop:runGame -Pargs="map:3"`
  或
  `./gradlew desktop:runGame -Pargs="--screen=map --layer=3"`

- 战斗界面：
  `./gradlew desktop:runGame -Pargs="battle"`

- 战斗失败弹窗：
  `./gradlew desktop:runGame -Pargs="battle-defeat"`

- 满药水奖励替换流程：
  `./gradlew desktop:runGame -Pargs="reward-potion"`

  该调试入口以满药水栏直接打开药水奖励，用于验证“丢弃一瓶后领取 / 跳过药水”。

- 商店界面：
  `./gradlew desktop:runGame -Pargs="shop"`

  `shop` 调试场景会创建带有 10 张战士卡的调试牌组，可覆盖商品购买、药水栏、弃牌选择/确认等非空 UI 状态。

- 事件界面：
  `./gradlew desktop:runGame -Pargs="event"`

- 指定事件界面：
  `./gradlew desktop:runGame -Pargs="--screen=event --event=投资"`

- 卡牌/藏品总览网格：
  `./gradlew desktop:runGame -Pargs="inventory"`

## 字体调试界面

字体调试界面会纵向列出：

- 多个字号：`0.75 / 1.0 / 1.25 / 1.5 / 1.8 / 2.2 / 3.0`
- 多种颜色：白、黑、金、红、青、绿
- 英文、中文、数字和符号样例
- 常用 UI 图片与实体图片缩略图

如果出现缺字、字符错位、边缘异常模糊或图片加载失败，可以优先进入此界面定位问题。
