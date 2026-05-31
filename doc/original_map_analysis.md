# 原版 C 项目地图生成逻辑分析

> 本文档分析 `FabricatedBook`（C + SDL2 版）的地图生成机制，
> 并与 `FabricatedBookRemake`（Java + LibGDX 版）进行对比，
> 为重构版的地图系统修复提供参考依据。

---

## 一、核心数据结构对比

### 1.1 C 版：列优先的稀疏网格

```c
#define MAX_LAYERS 6
#define MAX_WIDTH 5
#define MAX_LENGTH 9

typedef struct Layer {
    int width;                              // 每列最多节点数（1~width）
    int length;                             // 列数
    int num;                                // 层级编号（1-6）
    int line_num[MAX_LENGTH];               // 每列实际节点数
    Node *head, *tail;                      // 首/尾节点指针
    Node *nodes[MAX_LENGTH][MAX_WIDTH];     // 2D 节点数组（稀疏）
} Layer;
```

**关键设计：**
- `length` 是**列数**（水平方向），`width` 是**每列最多节点数**（垂直方向）
- `line_num[col]` 记录第 `col` 列有多少个节点（1 到 `width` 之间）
- `nodes[col][row]` 的地址空间是 `9×5 = 45` 个指针，但大部分是 NULL
- 每列的实际节点数随机：`line_num[i] = 1 + rand() % width`
- 第一列和最后一列固定 `line_num = 1`

### 1.2 Java 版（当前）：行优先的矩形网格

```java
// MapGraph.java
private List<List<Node>> grid;  // grid[row][col]

// MapConfig.java
private final int width;   // 列数
private final int height;  // 行数
```

**关键设计：**
- `height` 是**行数**（垂直方向），`width` 是**列数**（水平方向）
- `grid` 是一个 `height × width` 的完全填充矩形
- 每个格子都有一个 Node 对象（无 NULL）
- 起始位置固定在 `grid[0][width/2]`

### 1.3 根本差异

| 维度 | C 版（原版） | Java 版（Remake） |
|:----|:------------|:-----------------|
| 空间组织 | 列优先 `nodes[col][row]` | 行优先 `grid[row][col]` |
| 填充方式 | 稀疏（每列 1~width 个节点） | 密实（全填充矩形） |
| 网格形状 | 非矩形，各列高度不一 | 固定矩形 |
| 空洞 | ✅ 有空洞（NULL 占位） | ❌ 无空洞，每个格子有节点 |
| 视觉风格 | 杀戮尖塔风格，有机自然 | 呆板矩形排列 |

---

## 二、地图参数设置

### 2.1 C 版各层参数

```c
void init_every_layer() {
    srand(time(NULL));

    layers[0] = (Layer) {3, 4, 1};   // 层1: 荒野     width=3, length=4
    layers[1] = (Layer) {3, 5, 2};   // 层2: 森林     width=3, length=5
    layers[2] = (Layer) {4, 6, 3};   // 层3: 诡异秘林  width=4, length=6
    layers[3] = (Layer) {4, 7, 4};   // 层4: 迷雾     width=4, length=7
    layers[4] = (Layer) {4, 7, 5};   // 层5: 高塔     width=4, length=7
}
```

**每列节点数规则：** `line_num[i] = 1 + rand() % width`

| 层级 | width | 每列节点数 | 列数 | 总节点范围 |
|:---:|:----:|:----------:|:---:|:---------:|
| 荒野 | 3 | 1~3 | 4 | 4~10 |
| 森林 | 3 | 1~3 | 5 | 5~13 |
| 诡异秘林 | 4 | 1~4 | 6 | 6~22 |
| 迷雾 | 4 | 1~4 | 7 | 7~26 |
| 高塔 | 4 | 1~4 | 7 | 7~26 |

**对比 Java 版：** 全部为固定矩形，节点数固定：

| 层级 | Java版尺寸 | 节点数 |
|:---:|:---------:|:-----:|
| 荒野 | 4×3 | 12 |
| 森林 | 5×3 | 15 |
| 诡异秘林 | 6×4 | 24 |
| 迷雾 | 7×4 | 28 |
| 高塔 | 7×4 | 28 |

C 版的节点总数**随机**且远小于 Java 版，因为每列只有部分位置有节点。

### 2.2 起点与终点

**C 版：** 起点始终在 `nodes[0][0]`（第一列的唯一节点），终点在 `nodes[length-1][0]`（最后一列的唯一节点）

```c
layer->line_num[0] = 1;              // 第一列固定1个节点
layer->line_num[layer->length - 1] = 1;  // 最后一列固定1个节点
layer->nodes[0][0] = create_node(FIGHT);  // 起点
layer->nodes[layer->length - 1][0] = create_node(/* 终点类型 */);  // 终点
```

**Java 版：** 起点在 `grid[0][width/2]`（第一行中间），终点在 `grid[height-1][width/2]`

### 2.3 终点特殊列

C 版的第 4 层（迷雾）比较特殊——它的终点实际在倒数第二列，最后一列是命运抉择：

```c
case 3:  // layer_idx=3 → 第4层 迷雾
    layer->nodes[layer->length - 2][0] = create_node(BOSS);          // 倒数第二列 → Boss
    layer->line_num[layer->length - 2] = 1;                          // 该列固定1个节点
    layer->nodes[layer->length - 1][0] = create_node(DECISION);     // 最后一列 → 命运抉择
    layer->nodes[layer->length - 2][0]->nxt[0] = layer->nodes[layer->length - 1][0];  // Boss → 抉择
```

---

## 三、节点生成逻辑

### 3.1 C 版流程

```
for 每一列 (col = 1 to length-2):
    for 该列的每个节点 (row = 0 to line_num[col]):
        type = random_choice(概率表, 9)
        nodes[col][row] = create_node(type)

特殊处理：
    - 第 0 列（起点）→ 固定类型
    - 第 length-1 列（终点）→ 固定类型
    - 第 length-2 列（迷雾的 Boss）→ 固定类型
```

**核心差异：** C 版按**列**生成节点（每列独立随机），Java 版按**行**生成节点

### 3.2 概率权重表

C 版定义：

```c
int layer_probabilities[MAX_LAYERS][9] = {
    {60, 10, 0, 30, 0,  0,  0, 0, 0},   // 第1层 荒野
    {40, 20, 0, 10, 10, 10, 0, 0, 10},  // 第2层 森林
    {40, 20, 0, 10, 10, 10, 0, 0, 10},  // 第3层 诡异秘林
    {40, 20, 0, 10, 10, 10, 0, 0, 10},  // 第4层 迷雾
    {40, 20, 0, 10, 10, 10, 0, 0, 10},  // 第5层 高塔
    {0,  0,  0, 0,  0,  0,  0, 0, 100}  // 第6层 固定线路
};
```

9 个位置对应：`FIGHT, EMERGENCY, BOSS, UNEXPECTEDLY, REWARD, SHOP, ANOTHER_PATH, DECISION, SAFE_HOUSE`

**NodeType 枚举：**
```c
typedef enum {
    FIGHT = 1,        // 战斗
    EMERGENCY,        // 紧急作战
    BOSS,             // Boss
    UNEXPECTEDLY,     // 不期而遇
    REWARD,           // 得偿所愿
    SHOP,             // 商店
    ANOTHER_PATH,     // 另一条路
    DECISION,         // 命运抉择
    SAFE_HOUSE        // 安全屋
} NodeType;
```

**与 Java 版对比：**
- C 版第 2~5 层的概率完全相同（都是 40/20/10/10/10/10/0/0/10）
- C 版第 3 层（诡异秘林）有 10% SAFE_HOUSE，而 Java 版 MapConfig 设为 5%
- C 版没有 BOSS 概率（BOSS 是手动放置的，不是随机生成）
- C 版有 `ANOTHER_PATH` 类型，Java 版没有

---

## 四、节点连接算法（核心差异）

### 4.1 C 版的滑动窗口连接

这是原版地图的核心机制，创造了类似杀戮尖塔的有机连接：

```c
int tail = layer->length - (layer_idx == 3 ? 3 : 2);
// 最后一列的节点全部连接到终点列（nodes[tail+1][0]）
for (int i = 0; i < layer->line_num[tail]; i++) {
    layer->nodes[tail][i]->nxt_num = 1;
    layer->nodes[tail][i]->nxt[0] = layer->nodes[tail + 1][0];
}

// 中间列的滑动窗口连接
for (int i = 1; i < tail; i++) {
    int x = 0, y = 0;
    for (int j = 0; j < layer->line_num[i]; j++) {
        int size = layer->line_num[i + 1] - 1 - y;
        if (size > 0) {
            y = y + rand() % (size + 1);      // 随机扩展窗口
        }
        if (j == layer->line_num[i] - 1 && y < layer->line_num[i + 1] - 1) {
            y = layer->line_num[i + 1] - 1;    // 最后一个节点覆盖剩余
        }
        layer->nodes[i][j]->nxt_num = y - x + 1;
        for (int k = x; k <= y; k++) {
            layer->nodes[i][j]->nxt[k - x] = layer->nodes[i + 1][k];
        }
        x = y;  // 窗口滑动
    }
}
```

**算法效果：**
1. 每列从左到右处理
2. 当前列节点 `j` 从 `x` 到 `y` 连接下一列的多个节点
3. `y` 在随机范围内递增（随机扩展窗口）
4. 最后一个节点强制覆盖到下一列的末尾
5. 结果：连接线呈**扇形发散再汇聚**的有机形态

```
示意图（C版连接效果）：
    
    列0    列1      列2      列3      列4
    ┌─┐   ┌─┐      ┌─┐      ┌─┐      ┌─┐
    │S│──→│A│──┬──→│D│──┬──→│G│──┬──→│E│
    └─┘   ├─┤  │   ├─┤  │   ├─┤  │   └─┘
          │B│──┼──→│E│  │   │H│──┤
          └─┘  │   ├─┤  │   └─┘  │
               └──→│F│──┼────────┘
                   └─┘  │
                        └──→│I│
                            └─┘
```

### 4.2 Java 版的固定 ±1 连接

```java
// MapGraph.java
private void generateConnections() {
    for (int row = 0; row < grid.size() - 1; row++) {
        List<Node> currentRow = grid.get(row);
        List<Node> nextRow = grid.get(row + 1);
        for (int col = 0; col < currentRow.size(); col++) {
            Node node = currentRow.get(col);
            for (int dCol = -1; dCol <= 1; dCol++) {
                int targetCol = col + dCol;
                if (targetCol >= 0 && targetCol < nextRow.size()) {
                    node.addConnection(nextRow.get(targetCol));
                }
            }
        }
    }
}
```

**效果：** 每个节点固定连接到下一行的 `col-1, col, col+1`

```
示意图（Java版连接效果）：
    行0       行1       行2       行3
    ┌─┐       ┌─┐       ┌─┐       ┌─┐
    │S│──┬──→│A│──┬──→│D│──┬──→│G│
    └─┘  │   ├─┤  │   ├─┤  │   └─┘
         │   │B│──┤   │E│──┤
         │   └─┘  │   └─┘  │
         └───────→│C│──────→│F│
                  └─┘       └─┘
```

每行节点数相同，连接方式固定为 ±1，没有随机性和有机感。

---

## 五、第六层（特殊层）

C 版有第 6 层，是一个固定线性路径：

```c
void init_layer_six(Layer *layer) {
    layer->width = 1;
    layer->length = 4;
    layer->num = 6;
    for (int i = 0; i < 4; i++) {
        layer->line_num[i] = 1;       // 每列 1 个节点
    }
    // 固定节点序列：商店 → 精英 → 安全屋 → Boss
    layer->nodes[0][0] = create_node(SHOP);
    layer->nodes[1][0] = create_node(EMERGENCY);
    layer->nodes[2][0] = create_node(SAFE_HOUSE);
    layer->nodes[3][0] = create_node(BOSS);
    // 线性连接
    layer->nodes[0][0]->nxt[0] = layer->nodes[1][0];
    layer->nodes[1][0]->nxt[0] = layer->nodes[2][0];
    layer->nodes[2][0]->nxt[0] = layer->nodes[3][0];
}
```

Java 版没有第 6 层，只有 5 层。

---

## 六、环境效果在节点进入时触发

C 版在 `goin_nodes()` 中实时应用环境效果：

```c
void goin_nodes(SDL_Window *window, SDL_Renderer *renderer,
                Node *node, Layer *layer, Player *player) {
    // 第2层（森林）：非战斗节点扣金币
    if (layer->num == 2) {
        if (node->type != 1 && node->type != 2 && node->type != 3) {
            player->coin -= generate_random(10, 20);
        }
    }
    // 第3层（诡异秘林）：战斗/非战斗伤害修正
    if (layer->num == 3) {
        if (node->type != 1 && node->type != 2 && node->type != 3) {
            player->extra_damage++;  // 非战斗→+1
        } else {
            player->extra_damage--;  // 战斗→-1
        }
    }
    // 第4层（迷雾）：概率回血或扣血
    if (layer->num == 4) {
        if (generate_random(1, 2) == 1) {
            player_get_hp(player, generate_random(5, 30));
        } else {
            player->hp -= generate_random(1, 20);
        }
    }
    // 藏品"寡头"：非战斗节点获得金币
    if (main_collection[5][6].get && node->type >= 4) {
        player->coin += 20;
    }
    // 根据节点类型进入对应处理...
}
```

Java 版调试器中这些环境效果尚未实现（文档已说明为已知边界）。

---

## 七、Java 版重构方向建议

### 7.1 关键修复项

| 问题 | 当前行为 | 应改为 |
|:----|:--------|:------|
| 网格填充 | 完全填满矩形 | 列式稀疏网格，每列 1~W 个节点 |
| 方向 | 行优先 `grid[row][col]` | 列优先 `nodes[col][row]` |
| 首尾列 | 全部 FIGHT 强制 | 固定 1 个节点 |
| 连接算法 | 固定 ±1 连接 | 滑动窗口随机连接 |
| 空洞 | 无 | 稀疏网格自然产生空洞 |

### 7.2 建议的数据结构改造

```java
// 建议的新数据模型（参考 C 版设计）
class Layer {
    int width;                    // 每列最多节点数
    int length;                   // 列数
    int level;                    // 层级编号
    int[] lineNum;                // 每列实际节点数
    Node[][] nodes;               // nodes[col][row]，稀疏
    Node head, tail;              // 首尾节点
}

class Node {
    NodeType type;
    int col, row;                 // 列号、行内编号
    float x, y;                   // 屏幕坐标
    List<Node> nxt;               // 前向连接
    boolean visited, visible;
}
```

### 7.3 连接算法移植（伪代码）

```java
void generateConnections() {
    int tail = length - (isMistLayer ? 3 : 2);
    
    // 结尾列统一连接到终点
    for (Node node : nodes[tail]) {
        node.nxt.add(nodes[tail + 1][0]);
    }
    
    // 中间列滑动窗口连接
    for (int col = 1; col < tail; col++) {
        int x = 0, y = 0;
        for (int j = 0; j < lineNum[col]; j++) {
            int size = lineNum[col + 1] - 1 - y;
            if (size > 0) {
                y += random.nextInt(size + 1);
            }
            if (j == lineNum[col] - 1 && y < lineNum[col + 1] - 1) {
                y = lineNum[col + 1] - 1;
            }
            for (int k = x; k <= y; k++) {
                nodes[col][j].nxt.add(nodes[col + 1][k]);
            }
            x = y;
        }
    }
}
```

---

## 八、总结

| 特性 | C 版（原版） | Java 版（Remake 当前） |
|:----|:-----------|:---------------------|
| 网格结构 | 稀疏列式 | 密集矩形 |
| 有空洞 | ✅ | ❌ 全填充 |
| 首尾列 | 固定 1 节点 | 全部 FIGHT 强制 |
| 连接算法 | 滑动窗口随机 | 固定 ±1 |
| 节点随机 | 按列随机 | 按行随机 |
| 第 6 层 | ✅ 固定路线 | ❌ 不存在 |
| 迷雾双终点 | ✅ Boss + 抉择 | ❌ 仅 Boss |
| 环境效果 | 实时触发 | 待实现 |

> **结论：** Java 版的地图系统需要从**矩形网格模型**重构为**列式稀疏网格模型**，并移植 C 版的滑动窗口连接算法，才能还原杀戮尖塔风格的有机地图。
