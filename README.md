# Fabricated Book Remake

> 杀戮尖塔（Slay the Spire）Java + LibGDX 重构版  
> 原项目：C + SDL2 版本（[FabricatedBook](../FabricatedBook)）

---

## 一、项目概述

使用 **Java 17** + **LibGDX** 框架对原 C 版杀戮尖塔进行重构，旨在实现：

- **前后端分离** — 逻辑引擎与渲染层解耦
- **可扩展数值系统** — 卡牌、怪物、藏品数据外部化（JSON）
- **命令模式驱动战斗** — 所有战斗动作封装为独立指令，便于动画编排
- **组件化地图生成** — 概率权重驱动的随机地图

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────┐
│                     View Layer                          │
│   (LibGDX Scene2D / 动画 / 拖拽 / 粒子特效)             │
│                                                         │
│   只负责"显示"后端结果，不参与数值计算                    │
└────────────────────┬────────────────────────────────────┘
                     │ Action 执行结果 → 动画通知
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   Logic Engine                          │
│   (纯 Java，无图形依赖 / 战斗结算 / Buff 触发 / 随机数)  │
│                                                         │
│   CombatEngine → ActionManager → DamageCalculator       │
└────────────────────┬────────────────────────────────────┘
                     │ 读写
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│   (JSON / YAML / 卡牌 / 怪物 / 藏品 / 存档)             │
└─────────────────────────────────────────────────────────┘
```

### 1.1 展现层 (Front-end / View)

- 基于 **LibGDX Scene2D** 构建 UI
- `CardActor` — 卡牌的拖拽、缩放、旋转动画
- `BattleScreen` — 战斗场景布局与绘制
- 粒子特效系统 — 攻击命中、回血、Buff 触发等视觉反馈
- **不参与任何数值计算**，仅消费后端推送的执行结果

### 1.2 逻辑引擎 (Back-end / Logic)

- **纯 Java 实现**，零图形库依赖，支持单元测试
- 负责战斗结算、数值修正、Buff 生命周期、随机数生成
- 核心工作流：

```
InputHandler (玩家操作)
    → CombatEngine (解析指令)
    → ActionManager (排队执行)
    → DamageCalculator (数值修正)
    → ViewNotifier (通知前端播动画)
```

### 1.3 数据层 (Data Layer)

- 卡牌、怪物、藏品、地图配置 → **JSON 文件存储**
- 藏品和药水 → 数据对象实例化后进入商店、事件、奖励和存档
- 玩家存档 → 本地序列化
- 数据与逻辑分离，便于调试和用工具辅助修改

---

## 三、核心接口设计

### 3.1 战斗动作接口 — `CombatAction`

所有战斗动作（伤害、回血、抽牌、施加 Buff）实现此接口：

```java
public interface CombatAction {
    /** 执行具体的数值修改 */
    void execute();
    /** 用于 View 层判断动画是否播放完毕 */
    boolean isFinished();
}
```

### 3.2 Buff 生命周期钩子 — `BuffHook`

所有 Buff 效果通过钩子模式嵌入结算流程：

```java
public interface BuffHook {
    /** 受到伤害时触发，返回修正后的伤害值（处理 脆弱、抗性） */
    default int onDamageReceived(int damage, Entity source) { return damage; }
    /** 回合开始时触发（处理 中毒、凋零） */
    default void onTurnStart(Entity owner) {}
}
```

### 3.3 战斗结算流

以打出卡牌 "英雄登场" 为例：

```
玩家将卡牌拖向敌人
    ↓
InputHandler 捕获操作
    ↓
CombatEngine 根据 card.json 生成 CombatAction 队列：
    ├── DamageAction(AllEnemies, 12)
    ├── ApplyBuffAction(AllEnemies, Vulnerable, 2)
    └── GainEnergyAction(3)
    ↓
ActionManager 依次弹出 Action 并执行
    ├── 执行 DamageAction 前，遍历目标身上的 BuffHook
    │   └── 目标有"脆弱(Fragile)" → 伤害修正为 12 × 1.25
    ├── 执行 ApplyBuffAction
    └── 执行 GainEnergyAction
    ↓
ViewNotifier 每完成一个 Action，通知前端播放对应动画
```

---

## 四、主要系统设计

### 4.1 实体系统 (Entity System)

```
AbstractEntity
├── Player (战士)
│   ├── HP / MaxHP
│   ├── 格挡值 (Block)
│   ├── 能量 (Energy)
│   ├── 手牌 / 抽牌堆 / 弃牌堆 / 消耗牌堆
│   ├── 药水栏（最多 3 瓶）
│   ├── 藏品列表
│   └── List<Buff>
└── Enemy (怪物基类)
    ├── HP / MaxHP
    ├── 意图 (Intent)
    ├── 行动脚本
    └── List<Buff>
```

### 4.2 藏品与环境系统

采用 **观察者模式** 实现：

```
GlobalEventManager (事件总线)
    ├── 玩家移动事件 → 环境效果触发（如森林层扣金币）
    ├── OnCardUsed 事件 → 藏品响应（如"吸血鬼的尖牙"）
    └── OnTurnEnd 事件 → 持续性效果结算

RelicManager (藏品管理器)
    └── 每个藏品注册到对应事件
        ├── 主动藏品 → 监听 OnCombatStart
        ├── 触发藏品 → 监听 OnDamageDealt
        └── 回合藏品 → 监听 OnTurnEnd
```

**地图环境效果映射：**

| 地图 | 环境效果 |
|:----|:---------|
| 荒野 | 无 |
| 森林 | 进入非战斗节点损失金币，战斗获得金币增加 |
| 诡异秘林 | 每进战斗节点伤害-1，非战斗节点伤害+1（±3） |
| 迷雾 | 每次前进概率回血或扣血 |
| 高塔 | 无 |

### 4.3 地图生成器

```
MapConfig (区域配置)
├── width / height (地图尺寸)
├── startNodeType (起点类型)
├── endNodeType (终点类型)
└── nodeWeights (节点概率权重表)

NodeFactory (节点工厂)
└── 权重随机算法:
    P(Node) = Weight(Target) / Σ(Weight(All))

MapGraph (地图图结构)
└── 邻接表表示的图
    ├── 节点可见性控制
    └── 路径位移限制
```

**节点类型：**

| 类型 | 说明 |
|:----|:-----|
| 战斗 | 常规战斗，胜利获得金币和卡牌 |
| 紧急作战 | 有难度增益的战斗，胜利获得藏品+金币+卡牌 |
| Boss | 层底 Boss 战 |
| 不期而遇 | 随机事件 |
| 得偿所愿 | 选择藏品 |
| 诡异行商 | 商店 |
| 命运抉择 | 根据选择更改探索方向 |
| 安全屋 | 获得补给 |

### 4.4 藏品与环境系统

藏品不再是静态属性，而是 **数据驱动对象 + 事件订阅者**：

```java
RelicData (JSON DTO)
    -> RelicFactory
    -> DataRelic implements Relic
    -> RelicManager.addRelic(...)
```

当前已接入：

- 商店出售真实卡牌、藏品、药水。
- 事件 `relicId` 会实例化为真实藏品。
- 战斗奖励可掉落藏品和药水。
- 战斗内可使用药水，使用后从药水栏移除。
- 存档保存/恢复牌组、藏品 ID 和药水 ID。

---

## 五、数据持久化

### 5.1 游戏配置（JSON）

```
data/
├── cards/
│   ├── warrior.json        # 战士卡牌池
│   ├── mage.json           # 法师卡牌池
│   └── witch.json          # 女巫卡牌池
├── monsters/
│   ├── goblin.json         # 哥布林
│   ├── treeman.json        # 树人
│   └── boss.json           # BOSS 配置
├── relics.json             # 藏品数据
└── maps/
    ├── wilderness.json     # 荒野地图配置
    ├── forest.json         # 森林地图配置
    ├── mystic_forest.json  # 诡异秘林
    ├── mist.json           # 迷雾
    └── tower.json          # 高塔
```

### 5.2 玩家存档

```
saves/
└── save_<timestamp>.json   # 存档文件
    ├── 当前楼层/位置
    ├── 卡牌与藏品列表
    ├── 玩家属性快照
    └── 地图状态
```

---

## 六、推荐类层级结构

```
com.fabricatedbook
├── core
│   ├── engine
│   │   ├── CombatEngine.java      # 战斗引擎，解析指令、编排 Action
│   │   ├── ActionManager.java     # Action 队列管理与执行调度
│   │   ├── DamageCalculator.java  # 伤害计算器（Buff 叠加修正）
│   │   └── ViewNotifier.java      # 后端→前端通知接口
│   │
│   ├── entity
│   │   ├── AbstractEntity.java    # 实体基类（HP/Block/Buff）
│   │   ├── Player.java            # 玩家实体
│   │   ├── Enemy.java             # 怪物实体
│   │   └── EntityFactory.java     # 实体工厂（从 JSON 加载）
│   │
│   ├── action
│   │   ├── CombatAction.java      # 战斗动作接口
│   │   ├── DamageAction.java      # 伤害动作
│   │   ├── HealAction.java        # 回血动作
│   │   ├── ApplyBuffAction.java   # 施加 Buff
│   │   ├── GainEnergyAction.java  # 获得能量
│   │   └── DrawCardAction.java    # 抽牌动作
│   │
│   ├── buff
│   │   ├── BuffHook.java          # Buff 生命周期钩子
│   │   ├── Fragile.java           # 脆弱
│   │   ├── Weak.java              # 虚弱
│   │   ├── Poison.java            # 中毒
│   │   ├── Withering.java         # 凋零
│   │   └── Armor.java             # 装甲（格挡不消失）
│   │
│   ├── card
│   │   ├── Card.java              # 卡牌数据载体
│   │   ├── CardPool.java          # 卡牌池（按职业）
│   │   └── CardFactory.java       # 从 JSON 生成卡牌实例
│   │
│   ├── relic
│   │   ├── Relic.java             # 藏品接口
│   │   ├── RelicManager.java      # 藏品管理器（事件订阅）
│   │   └── EventBus.java          # 事件总线
│   │
│   ├── map
│   │   ├── MapConfig.java         # 地图配置（尺寸/概率/环境）
│   │   ├── MapGraph.java          # 邻接表图结构
│   │   ├── NodeFactory.java       # 节点工厂（权重随机）
│   │   └── Node.java              # 地图节点
│   │
│   └── event
│       ├── GameEvent.java         # 游戏事件基类
│       ├── OnDamageDealt.java     # 造成伤害事件
│       ├── OnCardUsed.java        # 使用卡牌事件
│       └── OnTurnEnd.java         # 回合结束事件
│
├── data
│   ├── DataLoader.java            # JSON 数据加载器
│   └── SaveManager.java           # 存档管理器
│
└── view
    ├── FabricBookGame.java        # LibGDX 入口
    ├── screen
    │   ├── TitleScreen.java       # 标题画面
    │   ├── MapScreen.java         # 地图探索画面
    │   ├── BattleScreen.java      # 战斗场景
    │   ├── ShopScreen.java        # 商店画面
    │   └── EventScreen.java       # 事件画面
    ├── actor
    │   ├── CardActor.java         # 卡牌 Actor（拖拽/缩放）
    │   ├── EnemyActor.java        # 怪物 Actor（血条/意图）
    │   ├── PlayerActor.java       # 玩家 Actor
    │   └── ButtonActor.java       # 按钮 Actor
    ├── animation
    │   ├── DamageNumber.java      # 掉血数字飘字
    │   ├── BuffIcon.java          # Buff 图标动画
    │   └── ParticleEffect.java    # 粒子特效
    └── ui
        ├── HandPanel.java         # 手牌面板
        ├── EnergyBar.java         # 能量条
        └── BuffBar.java           # Buff 状态栏
```

---

## 七、构建与运行

### 环境要求

- JDK 17+
- Gradle 8.x（或使用 Gradle Wrapper）
- LibGDX 1.12+

### 构建命令

```bash
# 桌面端运行
./gradlew desktop:run

# 打包
./gradlew desktop:dist
```

---

## 八、战斗流程时序

```
Player            InputHandler       CombatEngine       ActionManager      ViewNotifier        Enemy
  │                    │                   │                   │                 │                │
  │──拖拽卡牌至敌人──→│                   │                   │                 │                │
  │                    │──解析卡牌效果──→│                   │                 │                │
  │                    │                   │──生成Action队列──│                 │                │
  │                    │                   │                   │──执行Action 1──│──播放动画──→│
  │                    │                   │                   │──执行Action 2──│──播放动画──→│
  │                    │                   │                   │──执行Action 3──│──播放动画──→│
  │                    │                   │                   │                 │                │
  │←────更新UI状态─────────────────────────────────────────────────────────│                │
```

---

## 九、与 C 原版对应关系

| C 版（FabricatedBook） | Java 版（FabricatedBookRemake） | 改进 |
|:----------------------|:-------------------------------|:----|
| `game.c` 主循环 | `CombatEngine` + `ActionManager` | 命令模式解耦，支持动画排队 |
| `card.c` 硬编码卡牌 | `CardFactory` + JSON 配置 | 数据驱动，无需改代码调数值 |
| `fight.c` 战斗逻辑 | `DamageCalculator` + `BuffHook` | 钩子模式，新增 Buff 不需修改核心结算 |
| `map.c` 地图生成 | `NodeFactory` + `MapGraph` | 概率权重表可配置 |
| 事件硬编码 | `EventBus` + 订阅者模式 | 藏品和环境的响应解耦 |
| SDL2 直接渲染 | LibGDX Scene2D | 成熟的 UI 框架，支持动效 |
