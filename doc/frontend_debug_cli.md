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

## 可用入口

- 标题界面：
  `./gradlew desktop:runGame -Pargs="title"`

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

- 商店界面：
  `./gradlew desktop:runGame -Pargs="shop"`

- 事件界面：
  `./gradlew desktop:runGame -Pargs="event"`

- 指定事件界面：
  `./gradlew desktop:runGame -Pargs="--screen=event --event=投资"`

## 字体调试界面

字体调试界面会纵向列出：

- 多个字号：`0.75 / 1.0 / 1.25 / 1.5 / 1.8 / 2.2 / 3.0`
- 多种颜色：白、黑、金、红、青、绿
- 英文、中文、数字和符号样例
- 常用 UI 图片与实体图片缩略图

如果出现缺字、字符错位、边缘异常模糊或图片加载失败，可以优先进入此界面定位问题。
