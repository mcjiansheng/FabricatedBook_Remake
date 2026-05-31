package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.view.FabricBookGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MapScreen — 地图画面
 * <p>
 * 显示当前楼层的地图，节点以水平方向排列，支持横向拖动。
 * 当前可进入的节点高亮显示，不可进入的节点灰暗显示。
 * 连接的节点之间绘制线条。
 * 顶部显示玩家状态（HP、金币、药水）。
 * <p>
 * 引用方：TitleScreen（新建游戏）、EventScreen/FightScreen（事件结束后返回）
 */
public class MapScreen implements Screen {

    // ====== 节点类型常量（与 C 版保持一致）======
    public static final int FIGHT = 1;
    public static final int EMERGENCY = 2;
    public static final int BOSS = 3;
    public static final int UNEXPECTEDLY = 4;
    public static final int REWARD = 5;
    public static final int SHOP = 6;
    public static final int ANOTHER_PATH = 7;
    public static final int DECISION = 8;
    public static final int SAFE_HOUSE = 9;

    // ====== 层级配置 ======
    private static final int[] LAYER_LENGTHS = {4, 5, 6, 7, 7};
    private static final int[] LAYER_WIDTHS = {3, 3, 4, 4, 4};
    private static final int[] LAYER_START_TYPES = {FIGHT, FIGHT, REWARD, EMERGENCY, UNEXPECTEDLY};
    private static final int[] LAYER_END_TYPES = {DECISION, SHOP, BOSS, BOSS, BOSS};
    private static final int LAYER_END_BOSS_COL = 5;

    // 每层节点概率（与 C 版保持一致）
    private static final int[][] LAYER_PROBABILITIES = {
            {60, 10, 0, 30, 0, 0, 0, 0, 0},
            {40, 20, 0, 10, 10, 10, 0, 0, 10},
            {40, 20, 0, 10, 10, 10, 0, 0, 10},
            {40, 20, 0, 10, 10, 10, 0, 0, 10},
            {40, 20, 0, 10, 10, 10, 0, 0, 10}
    };

    // ====== 节点贴图索引 ======
    private static final int[] NODE_TYPE_TO_TEXTURE = {
            0, // 0 unused
            1, // FIGHT -> img/fight.png
            2, // EMERGENCY -> img/Emergency.png
            3, // BOSS -> img/boss.png
            4, // UNEXPECTEDLY -> img/unexpectedly.png
            5, // REWARD -> img/reward.png
            6, // SHOP -> img/shop.png
            7, // ANOTHER_PATH -> img/another_path.png
            8, // DECISION -> img/decision.png
            9  // SAFE_HOUSE -> img/safe_house.png
    };

    // ====== 节点内部类（与 C 版 struct Node 对应）======
    private static class MapNode {
        int x, y;           // 屏幕坐标（由 layout 计算）
        int col, row;       // 列，行
        int type;           // 节点类型
        int nxtNum;
        MapNode[] nxt;
        boolean accessible; // 当前是否可进入
        boolean visited;    // 是否已访问过

        MapNode(int type) {
            this.type = type;
            this.nxtNum = 0;
            this.nxt = new MapNode[4];
            this.accessible = false;
            this.visited = false;
        }
    }

    // ====== 字段 ======
    private final FabricBookGame game;
    private final Player player;
    private int currentLayerIdx; // 当前楼层索引 0-4
    private MapNode[][][] allLayers; // [layer][col][row]
    private int[][] layerNodeCounts; // [layer][col] 每列节点数
    private MapNode currentNode; // 当前所在的节点
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private Random random;

    // 贴图资源
    private Texture[] nodeTextures; // index 1-9
    private Texture bgTexture;

    // 拖动状态
    private float scrollX;
    private float lastTouchX;
    private boolean isDragging;
    private static final float SCROLL_SPEED = 1.0f;
    private static final float STEP_X = 140f;
    private static final float NODE_SIZE = 48f;

    // UI 顶栏
    private static final float TOP_BAR_HEIGHT = 35f;

    // 药水
    private List<PotionInfo> potions;

    public static class PotionInfo {
        public String name;
        public PotionInfo(String name) { this.name = name; }
    }

    public MapScreen(FabricBookGame game, Player player) {
        this.game = game;
        this.player = player;
        this.currentLayerIdx = 0;
        this.currentNode = null;
        this.scrollX = 0;
        this.random = new Random();
        this.potions = new ArrayList<>();

        // 初始化药水
        potions.add(new PotionInfo("回血药水"));
        potions.add(new PotionInfo("攻击药水"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);

        loadTextures();
        generateAllLayers();
    }

    /** 加载节点贴图 */
    private void loadTextures() {
        nodeTextures = new Texture[10];
        String[] files = {"", "fight.png", "Emergency.png", "boss.png", "unexpectedly.png",
                "reward.png", "shop.png", "another_path.png", "decision.png", "safe_house.png"};
        for (int i = 1; i <= 9; i++) {
            try {
                nodeTextures[i] = new Texture("img/" + files[i]);
            } catch (Exception e) {
                nodeTextures[i] = null;
            }
        }
        try {
            bgTexture = new Texture("img/background.png");
        } catch (Exception e) {
            bgTexture = null;
        }
    }

    /** 生成所有层的地图 */
    private void generateAllLayers() {
        allLayers = new MapNode[5][][];
        layerNodeCounts = new int[5][];
        for (int layer = 0; layer < 5; layer++) {
            generateLayer(layer);
        }
    }

    /** 生成单层地图（对应 C 版 init_layer） */
    private void generateLayer(int layerIdx) {
        int len = LAYER_LENGTHS[layerIdx];
        int width = LAYER_WIDTHS[layerIdx];
        int[] lineNum = new int[len];
        layerNodeCounts[layerIdx] = lineNum;

        // 每列随机节点数
        for (int col = 1; col < len - 1; col++) {
            lineNum[col] = 1 + random.nextInt(width);
        }
        lineNum[0] = 1;
        lineNum[len - 1] = 1;

        // 如果是迷雾层 (layerIdx == 3)，倒数第二列也是1
        if (layerIdx == 3) {
            lineNum[len - 2] = 1;
        }

        MapNode[][] layer = new MapNode[len][];
        allLayers[layerIdx] = layer;

        // 创建节点
        for (int col = 0; col < len; col++) {
            layer[col] = new MapNode[lineNum[col]];
            for (int row = 0; row < lineNum[col]; row++) {
                // 特殊列：起点/终点
                if (col == 0) {
                    layer[col][row] = new MapNode(LAYER_START_TYPES[layerIdx]);
                } else if (col == len - 1) {
                    layer[col][row] = new MapNode(LAYER_END_TYPES[layerIdx]);
                } else if (layerIdx == 3 && col == len - 2) {
                    layer[col][row] = new MapNode(BOSS);
                } else {
                    int type = randomChoice(LAYER_PROBABILITIES[layerIdx]);
                    layer[col][row] = new MapNode(type);
                }
                layer[col][row].col = col;
                layer[col][row].row = row;
            }
        }

        // 建立连接（列间连接）
        int tail = len - (layerIdx == 3 ? 3 : 2);
        for (int col = 0; col < len - 1; col++) {
            if (col == 0) {
                // 起点连接到第一列所有节点
                for (int r = 0; r < lineNum[1]; r++) {
                    layer[0][0].nxt[layer[0][0].nxtNum++] = layer[1][r];
                }
            } else if (col < tail) {
                // 中间列：按 C 版算法连接
                connectColumns(layer, lineNum, col, tail);
            } else if (col == tail) {
                // 最后一列普通节点连接到终点
                for (int r = 0; r < lineNum[tail]; r++) {
                    if (layer[col][r] != null) {
                        layer[col][r].nxt[layer[col][r].nxtNum++] = layer[len - 1][0];
                    }
                }
            }
        }

        // 计算布局坐标
        layoutLayer(layerIdx);
    }

    /** 连接相邻两列（对应 C 版连接算法） */
    private void connectColumns(MapNode[][] layer, int[] lineNum, int col, int tail) {
        if (col >= tail) return;
        int x = 0, y = 0;
        for (int j = 0; j < lineNum[col]; j++) {
            int size = Math.max(0, lineNum[col + 1] - 1 - y);
            if (size > 0) {
                y += random.nextInt(size + 1);
            }
            if (j == lineNum[col] - 1 && y < lineNum[col + 1] - 1) {
                y = lineNum[col + 1] - 1;
            }
            int connectCount = y - x + 1;
            for (int k = x; k <= y; k++) {
                if (k < layer[col + 1].length) {
                    layer[col][j].nxt[layer[col][j].nxtNum++] = layer[col + 1][k];
                }
            }
            x = y;
        }
    }

    /** 布局计算（对应 C 版 init_map_printer） */
    private void layoutLayer(int layerIdx) {
        MapNode[][] layer = allLayers[layerIdx];
        int[] lineNum = layerNodeCounts[layerIdx];
        int len = LAYER_LENGTHS[layerIdx];

        float startX = 50;

        for (int col = 0; col < len; col++) {
            int size = lineNum[col];
            float stepY;
            float startY;

            if (size == 3) {
                stepY = FabricBookGame.SCREEN_HEIGHT / 4f;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f - stepY;
            } else if (size == 2) {
                stepY = FabricBookGame.SCREEN_HEIGHT / 3f;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f - stepY / 2;
            } else if (size == 4) {
                stepY = FabricBookGame.SCREEN_HEIGHT / 5f;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f - stepY * 1.5f;
            } else {
                stepY = 0;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f;
            }

            for (int row = 0; row < size; row++) {
                layer[col][row].x = (int) startX;
                layer[col][row].y = (int) startY;
                startY += stepY;
            }
            startX += STEP_X;
        }
    }

    /** 权重随机选节点类型 */
    private int randomChoice(int[] probabilities) {
        int total = 0;
        for (int p : probabilities) total += p;
        int r = random.nextInt(total);
        int sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (r < sum) return i + 1;
        }
        return FIGHT;
    }

    @Override
    public void show() {
        batch = game.getBatch();
        font = game.getFont();
        shapeRenderer = new ShapeRenderer();

        // 设置当前可访问节点
        updateAccessibleNodes();
    }

    /** 更新当前可访问节点 */
    private void updateAccessibleNodes() {
        MapNode[][] layer = allLayers[currentLayerIdx];

        // 全部设为不可访问
        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                if (layer[col][row] != null) {
                    layer[col][row].accessible = false;
                }
            }
        }

        // 设置当前节点指向的下个节点为可访问
        if (currentNode == null) {
            // 还没进入任何节点，起点可访问
            if (layer[0][0] != null) layer[0][0].accessible = true;
        } else {
            for (int i = 0; i < currentNode.nxtNum; i++) {
                if (currentNode.nxt[i] != null) {
                    currentNode.nxt[i].accessible = true;
                }
            }
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput(delta);

        batch.begin();

        // 绘制背景
        if (bgTexture != null) {
            batch.draw(bgTexture, 0, TOP_BAR_HEIGHT,
                    FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT - TOP_BAR_HEIGHT);
        }

        batch.end();

        // 绘制地图连接线
        drawConnectionLines();

        batch.begin();

        // 绘制节点
        drawNodes();

        batch.end();

        // 绘制顶栏（在 batch.end() 后绘制，使用 shapeRenderer + batch）
        drawTopBar();
    }

    /** 处理输入（横向拖动） */
    private void handleInput(float delta) {
        if (Gdx.input.isTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();

            // 如果点在顶栏区域内，不处理地图拖动
            if (touchY > FabricBookGame.SCREEN_HEIGHT - TOP_BAR_HEIGHT) {
                return;
            }

            if (!isDragging) {
                lastTouchX = touchX;
                isDragging = true;
            } else {
                float dx = touchX - lastTouchX;
                if (Math.abs(dx) > 5) {
                    // 拖动
                    scrollX += dx * SCROLL_SPEED;
                    lastTouchX = touchX;
                    // 限制滚动范围
                    MapNode[][] layer = allLayers[currentLayerIdx];
                    if (layer != null && layer.length > 0) {
                        float maxScroll = (layer.length - 1) * STEP_X;
                        if (scrollX > 50) scrollX = 50;
                        if (scrollX < -(maxScroll - FabricBookGame.SCREEN_WIDTH + 50)) {
                            scrollX = -(maxScroll - FabricBookGame.SCREEN_WIDTH + 50);
                        }
                    }
                }
            }
        } else {
            if (isDragging) {
                // 检测点击（拖动结束后检测是否点击了节点）
                float touchX = Gdx.input.getX();
                float touchY = Gdx.input.getY();
                checkNodeClick(touchX, touchY);
            }
            isDragging = false;
        }
    }

    /** 检测节点点击 */
    private void checkNodeClick(float screenX, float screenY) {
        MapNode[][] layer = allLayers[currentLayerIdx];
        float worldX = screenX - scrollX;

        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node != null && node.accessible) {
                    float dx = node.x - worldX;
                    float dy = node.y - screenY;
                    if (Math.abs(dx) < NODE_SIZE && Math.abs(dy) < NODE_SIZE) {
                        // 进入节点
                        enterNode(node);
                        return;
                    }
                }
            }
        }
    }

    /** 进入节点（根据类型进入不同场景） */
    private void enterNode(MapNode node) {
        currentNode = node;
        node.visited = true;
        updateAccessibleNodes();

        // 根据节点类型切换到对应场景
        switch (node.type) {
            case FIGHT:
            case EMERGENCY:
            case BOSS:
                // 进入战斗
                game.setScreen(new BattleScreen(game, createCombatEngine(), player,
                        createEnemiesFor(node.type)));
                break;
            case SHOP:
                // 进入商店
                ShopManager shopManager = new ShopManager(player, new RelicManager(player));
                shopManager.generateItems();
                game.setScreen(new ShopScreen(game, player, shopManager));
                break;
            case UNEXPECTEDLY:
                // 进入事件
                game.setScreen(new EventScreen(game, player, randomEventName()));
                break;
            case REWARD:
                // 宝箱奖励
                game.setScreen(new EventScreen(game, player, "好诗歪诗"));
                break;
            case SAFE_HOUSE:
                // 安全屋
                game.setScreen(new EventScreen(game, player, "人生意义"));
                break;
            case DECISION:
                // 命运抉择
                game.setScreen(new EventScreen(game, player, "投资"));
                break;
        }
    }

    private CombatEngine createCombatEngine() {
        CombatEngine engine = new CombatEngine();
        engine.setRelicManager(new RelicManager(player));
        return engine;
    }

    private List<Enemy> createEnemiesFor(int nodeType) {
        List<Enemy> enemies = new ArrayList<>();
        if (nodeType == BOSS) {
            enemies.add(new Enemy("boss_training", "训练首领", 60,
                    List.of("atk8", "def6", "atk10")));
        } else if (nodeType == EMERGENCY) {
            enemies.add(new Enemy("elite_training", "精英训练假人", 42,
                    List.of("atk7", "atk5x2", "def8")));
        } else {
            enemies.add(EntityFactory.createSimpleEnemy("training_dummy",
                    "训练假人", 32));
        }
        return enemies;
    }

    private String randomEventName() {
        List<String> eventNames = new EventHandler().getEventNames();
        return eventNames.get(random.nextInt(eventNames.size()));
    }

    /** 绘制节点之间的连接线 */
    private void drawConnectionLines() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node == null || node.nxtNum == 0) continue;

                // 确定线条颜色
                boolean isConnectedToAccessible = false;
                for (int i = 0; i < node.nxtNum; i++) {
                    if (node.nxt[i] != null && node.nxt[i].accessible) {
                        isConnectedToAccessible = true;
                        break;
                    }
                }

                for (int i = 0; i < node.nxtNum; i++) {
                    MapNode next = node.nxt[i];
                    if (next == null) continue;

                    if (node.accessible || isConnectedToAccessible) {
                        shapeRenderer.setColor(0.8f, 0.7f, 0.3f, 1f); // 金色
                    } else {
                        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.5f); // 灰色
                    }
                    shapeRenderer.line(
                            node.x + scrollX, node.y,
                            next.x + scrollX, next.y
                    );
                }
            }
        }

        shapeRenderer.end();
    }

    /** 绘制所有节点 */
    private void drawNodes() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null) return;

        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node == null) continue;

                float drawX = node.x + scrollX - NODE_SIZE / 2;
                float drawY = node.y - NODE_SIZE / 2;

                // 获取对应贴图
                Texture tex = null;
                int texIdx = NODE_TYPE_TO_TEXTURE[node.type];
                if (texIdx >= 1 && texIdx <= 9) {
                    tex = nodeTextures[texIdx];
                }

                // 根据节点状态设置透明度
                if (node.accessible) {
                    // 可进入节点：正常显示
                    batch.setColor(1, 1, 1, 1);
                } else if (node.visited) {
                    // 已访问节点：半透明
                    batch.setColor(0.5f, 0.5f, 0.5f, 0.6f);
                } else {
                    // 不可访问节点：暗色
                    batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
                }

                if (tex != null) {
                    batch.draw(tex, drawX, drawY, NODE_SIZE, NODE_SIZE);
                }

                // 如果是可访问节点，加上高亮边框
                if (node.accessible) {
                    // 无法在 batch 中绘制边框，在之后的 shapeRenderer 中处理
                }

                batch.setColor(1, 1, 1, 1);
            }
        }
    }

    /** 绘制顶部状态栏 */
    private void drawTopBar() {
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // 黑色半透明背景
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.9f);
        shapeRenderer.rect(0, FabricBookGame.SCREEN_HEIGHT - TOP_BAR_HEIGHT,
                FabricBookGame.SCREEN_WIDTH, TOP_BAR_HEIGHT);
        shapeRenderer.end();

        batch.begin();

        // HP
        font.draw(batch, "❤ " + player.getHp() + "/" + player.getMaxHp(),
                10, FabricBookGame.SCREEN_HEIGHT - 10);

        // 金币
        font.draw(batch, "💰 " + player.getGold(),
                180, FabricBookGame.SCREEN_HEIGHT - 10);

        // 药水
        float potionX = 350;
        for (int i = 0; i < potions.size(); i++) {
            font.draw(batch, "🧪 " + potions.get(i).name,
                    potionX, FabricBookGame.SCREEN_HEIGHT - 10);
            potionX += 100;
        }

        // 当前层名
        String[] layerNames = {"荒野", "森林", "诡异秘林", "迷雾", "高塔"};
        font.draw(batch, "🌍 " + layerNames[currentLayerIdx],
                FabricBookGame.SCREEN_WIDTH - 200, FabricBookGame.SCREEN_HEIGHT - 10);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}
    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        for (int i = 1; i <= 9; i++) {
            if (nodeTextures[i] != null) nodeTextures[i].dispose();
        }
        if (bgTexture != null) bgTexture.dispose();
    }
}
