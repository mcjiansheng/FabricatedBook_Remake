package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.TopStatusBar;

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
    private final GameRunState runState;
    private final Player player;
    private int currentLayerIdx; // 当前楼层索引 0-4
    private MapNode[][][] allLayers; // [layer][col][row]
    private int[][] layerNodeCounts; // [layer][col] 每列节点数
    private MapNode currentNode; // 当前所在的节点
    private MapNode activeNode;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private TopStatusBar topStatusBar;
    private Stage uiStage;
    private Random random;

    // 贴图资源
    private Texture[] nodeTextures; // index 1-9
    private Texture bgTexture;

    // 拖动状态
    private float scrollX;
    private float scrollY;
    private float lastTouchX;
    private float lastTouchY;
    private float dragDistance;
    private boolean isDragging;
    private static final float SCROLL_SPEED = 1.0f;
    private static final float STEP_X = 260f;
    private static final float NODE_WIDTH = 150f;
    private static final float NODE_HEIGHT = 86f;
    private static final float MAP_LEFT_PAD = 150f;
    private static final float CONNECTION_LINE_WIDTH = 3.5f;
    private static final float NODE_FRAME_THICKNESS = 4f;
    private static final float NODE_FRAME_GAP = 6f;
    private static final float TOP_BUTTON_W = 150f;

    // UI 顶栏
    private static final float TOP_BAR_HEIGHT = 52f;
    private static final String[] LAYER_NAMES = {"荒野", "森林", "诡异秘林", "迷雾", "高塔"};
    private static final String[] LAYER_EFFECTS = {
            "当前层效果：无",
            "当前层效果：奖励与商店出现",
            "当前层效果：Boss 节点出现",
            "当前层效果：路线更窄，Boss 连战",
            "当前层效果：最终高塔"
    };
    private float layerIntroTimer;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;

    public MapScreen(FabricBookGame game, Player player) {
        this(game, new GameRunState(System.currentTimeMillis(), player), 1);
    }

    public MapScreen(FabricBookGame game, Player player, int initialLayer) {
        this(game, new GameRunState(System.currentTimeMillis(), player), initialLayer);
    }

    public MapScreen(FabricBookGame game, GameRunState runState) {
        this(game, runState, runState.getCurrentLayerIdx() + 1);
    }

    public MapScreen(FabricBookGame game, GameRunState runState, int initialLayer) {
        this.game = game;
        this.runState = runState;
        this.player = runState.getPlayer();
        this.currentLayerIdx = Math.max(0, Math.min(4, initialLayer - 1));
        this.runState.setCurrentLayerIdx(this.currentLayerIdx);
        this.currentNode = null;
        this.activeNode = null;
        this.scrollX = 0;
        this.scrollY = 0;
        this.random = runState.randomFor("map-layout");
        this.layerIntroTimer = 2.2f;
        this.game.setCurrentRun(runState);

        viewport = ResponsiveViewport.create();
        camera = (OrthographicCamera) viewport.getCamera();

        loadTextures();
        generateAllLayers();
        restoreRunPosition();
    }

    /** 加载节点贴图 */
    private void loadTextures() {
        nodeTextures = new Texture[10];
        String[] files = {"", "fight.png", "Emergency.png", "boss.png", "unexpectedly.png",
                "reward.png", "shop.png", "another_path.png", "decision.png", "safe_house.png"};
        for (int i = 1; i <= 9; i++) {
            try {
                nodeTextures[i] = new Texture("img/" + files[i]);
                nodeTextures[i].setFilter(TextureFilter.Linear, TextureFilter.Linear);
            } catch (Exception e) {
                nodeTextures[i] = null;
            }
        }
        bgTexture = null;
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
            lineNum[col] = 1 + runState.randomFor("map-line-count", layerIdx, col)
                    .nextInt(width);
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
                    int type = randomChoice(LAYER_PROBABILITIES[layerIdx],
                            runState.randomFor("map-node-type", layerIdx, col, row));
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
                connectColumns(layerIdx, layer, lineNum, col, tail);
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
    private void connectColumns(int layerIdx, MapNode[][] layer, int[] lineNum,
                                int col, int tail) {
        if (col >= tail) return;
        int x = 0, y = 0;
        for (int j = 0; j < lineNum[col]; j++) {
            int size = Math.max(0, lineNum[col + 1] - 1 - y);
            if (size > 0) {
                y += runState.randomFor("map-connection", layerIdx, col, j)
                        .nextInt(size + 1);
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

        float startX = MAP_LEFT_PAD;

        for (int col = 0; col < len; col++) {
            int size = lineNum[col];
            float stepY;
            float startY;

            if (size == 3) {
                stepY = 200f;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f - stepY;
            } else if (size == 2) {
                stepY = 230f;
                startY = FabricBookGame.SCREEN_HEIGHT / 2f - stepY / 2;
            } else if (size == 4) {
                stepY = 160f;
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
        return randomChoice(probabilities, random);
    }

    private int randomChoice(int[] probabilities, Random random) {
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
        glyphLayout = new GlyphLayout();
        shapeRenderer = new ShapeRenderer();
        topStatusBar = new TopStatusBar(player, font);
        uiStage = new Stage(viewport);
        Gdx.input.setInputProcessor(uiStage);

        // 设置当前可访问节点
        updateAccessibleNodes();
        game.autosaveCurrentRun();
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
        Gdx.gl.glClearColor(0.78f, 0.78f, 0.78f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        handleInput(delta);
        if (layerIntroTimer > 0) {
            layerIntroTimer -= delta;
        }

        batch.begin();

        batch.end();

        // 绘制地图连接线
        drawConnectionLines();

        batch.begin();

        // 绘制节点
        drawNodes();

        batch.end();

        // 绘制可选/已访问节点边框
        drawNodeFrames();

        // 绘制顶栏（在 batch.end() 后绘制，使用 shapeRenderer + batch）
        drawTopBar();
        drawLayerIntro();
        if (uiStage != null) {
            uiStage.act(delta);
            uiStage.draw();
        }
    }

    /** 处理输入（横向拖动） */
    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
            return;
        }
        if (escapeMenu != null && escapeMenu.hasParent()) {
            return;
        }
        if (Gdx.input.isTouched()) {
            Vector2 touch = toWorld(Gdx.input.getX(), Gdx.input.getY());

            // 如果点在顶栏区域内，不处理地图拖动
            if (touch.y > FabricBookGame.SCREEN_HEIGHT - TOP_BAR_HEIGHT) {
                if (Gdx.input.justTouched()) {
                    handleTopBarClick(touch.x);
                }
                return;
            }

            if (!isDragging) {
                lastTouchX = touch.x;
                lastTouchY = touch.y;
                dragDistance = 0;
                isDragging = true;
            } else {
                float dx = touch.x - lastTouchX;
                float dy = touch.y - lastTouchY;
                dragDistance += Math.abs(dx) + Math.abs(dy);
                if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                    // 拖动
                    scrollX += dx * SCROLL_SPEED;
                    scrollY += dy * SCROLL_SPEED;
                    lastTouchX = touch.x;
                    lastTouchY = touch.y;
                    // 限制滚动范围
                    clampScroll();
                }
            }
        } else {
            if (isDragging && dragDistance < 12f) {
                // 检测点击（拖动结束后检测是否点击了节点）
                Vector2 touch = toWorld(Gdx.input.getX(), Gdx.input.getY());
                checkNodeClick(touch.x, touch.y);
            }
            isDragging = false;
        }
    }

    private Vector2 toWorld(float screenX, float screenY) {
        return viewport.unproject(new Vector2(screenX, screenY));
    }

    private void handleTopBarClick(float touchX) {
        float cardsX = FabricBookGame.SCREEN_WIDTH - 330;
        float relicsX = FabricBookGame.SCREEN_WIDTH - 170;
        if (touchX >= cardsX && touchX <= cardsX + TOP_BUTTON_W) {
            game.setScreen(new InventoryScreen(game, player, this));
        } else if (touchX >= relicsX && touchX <= relicsX + TOP_BUTTON_W) {
            game.setScreen(new InventoryScreen(game, player, this));
        }
    }

    /** 检测节点点击 */
    private void checkNodeClick(float screenX, float screenY) {
        MapNode[][] layer = allLayers[currentLayerIdx];
        float worldX = screenX - scrollX;
        float worldY = screenY - scrollY;

        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node != null && node.accessible) {
                    if (Math.abs(node.x - worldX) <= nodeDrawWidth(node) / 2f
                            && Math.abs(node.y - worldY) <= nodeDrawHeight(node) / 2f) {
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
        // 根据节点类型切换到对应场景
        switch (node.type) {
            case FIGHT:
            case EMERGENCY:
            case BOSS:
                activeNode = node;
                runState.beginCombat(toRef(node));
                game.autosaveCurrentRun();
                // 进入战斗
                game.setScreen(new BattleScreen(game, createCombatEngine(), player,
                        createEnemiesFor(node.type), this));
                break;
            case SHOP:
                markNodeEntered(node);
                // 进入商店
                ShopManager shopManager = new ShopManager(player, new RelicManager(player),
                        runState.getSeed(), "shop:" + nodeKey(node));
                shopManager.generateItems();
                game.autosaveCurrentRun();
                game.setScreen(new ShopScreen(game, player, shopManager, this));
                break;
            case UNEXPECTEDLY:
                markNodeEntered(node);
                game.autosaveCurrentRun();
                // 进入事件
                game.setScreen(new EventScreen(game, player, randomEventName(), this,
                        runState.randomFor("event-result", nodeKey(node))));
                break;
            case REWARD:
                markNodeEntered(node);
                game.autosaveCurrentRun();
                // 宝箱奖励
                game.setScreen(new EventScreen(game, player, "好诗歪诗", this,
                        runState.randomFor("event-result", nodeKey(node))));
                break;
            case SAFE_HOUSE:
                markNodeEntered(node);
                game.autosaveCurrentRun();
                // 安全屋
                game.setScreen(new EventScreen(game, player, "人生意义", this,
                        runState.randomFor("event-result", nodeKey(node))));
                break;
            case DECISION:
                markNodeEntered(node);
                game.autosaveCurrentRun();
                // 命运抉择
                game.setScreen(new EventScreen(game, player, "投资", this,
                        runState.randomFor("event-result", nodeKey(node))));
                break;
        }
    }

    private void markNodeEntered(MapNode node) {
        currentNode = node;
        activeNode = null;
        runState.clearCombatState();
        node.visited = true;
        runState.setCompletedNode(toRef(node));
        updateAccessibleNodes();
        applyNodeEntryRelics(node.type);
    }

    private GameRunState.NodeRef toRef(MapNode node) {
        if (node == null) return null;
        return new GameRunState.NodeRef(currentLayerIdx, node.col, node.row, node.type);
    }

    private String nodeKey(MapNode node) {
        if (node == null) return currentLayerIdx + ":none";
        return currentLayerIdx + ":" + node.col + ":" + node.row + ":" + node.type;
    }

    private void restoreRunPosition() {
        currentLayerIdx = runState.getCurrentLayerIdx();
        GameRunState.NodeRef completed = runState.getCompletedNode();
        if (completed != null) {
            currentLayerIdx = completed.layer;
            runState.setCurrentLayerIdx(currentLayerIdx);
            currentNode = findNode(completed);
            if (currentNode != null) {
                currentNode.visited = true;
            }
        }
        activeNode = null;
        runState.clearCombatState();
    }

    private MapNode findNode(GameRunState.NodeRef ref) {
        if (ref == null || ref.layer < 0 || ref.layer >= allLayers.length) return null;
        MapNode[][] layer = allLayers[ref.layer];
        if (ref.col < 0 || ref.col >= layer.length) return null;
        if (ref.row < 0 || ref.row >= layer[ref.col].length) return null;
        return layer[ref.col][ref.row];
    }

    private void toggleEscapeMenu() {
        if (escapeMenu != null && escapeMenu.hasParent()) {
            escapeMenu.remove();
            escapeMenu = null;
            return;
        }
        escapeMenu = EscapeMenu.show(uiStage, game, () -> escapeMenu = null);
    }

    /** 当前节点结算完成后回到地图，必要时进入下一层。 */
    public void completeCurrentNodeAndReturn() {
        if (activeNode != null) {
            activeNode.visited = true;
            currentNode = activeNode;
            runState.completeActiveNode();
            runState.setCompletedNode(toRef(currentNode));
            activeNode = null;
        } else if (currentNode != null) {
            runState.setCompletedNode(toRef(currentNode));
        }
        if (currentNode != null && currentNode.nxtNum == 0 && currentLayerIdx < allLayers.length - 1) {
            currentLayerIdx++;
            runState.setCurrentLayerIdx(currentLayerIdx);
            currentNode = null;
            runState.setCompletedNode(null);
            scrollX = 0;
            scrollY = 0;
            layerIntroTimer = 2.2f;
        }
        updateAccessibleNodes();
        game.autosaveCurrentRun();
        game.setScreen(this);
    }

    private void clampScroll() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null || layer.length == 0) return;
        float mapWidth = MAP_LEFT_PAD * 2 + (layer.length - 1) * STEP_X + NODE_WIDTH;
        float minX = Math.min(0, FabricBookGame.SCREEN_WIDTH - mapWidth);
        if (scrollX > 80) scrollX = 80;
        if (scrollX < minX - 80) scrollX = minX - 80;
        if (scrollY > 90) scrollY = 90;
        if (scrollY < -90) scrollY = -90;
    }

    private CombatEngine createCombatEngine() {
        MapNode node = activeNode != null ? activeNode : currentNode;
        CombatEngine engine = new CombatEngine(runState.getSeed(), "combat:" + nodeKey(node));
        player.setCurrentFloor(currentLayerIdx + 1);
        if (node != null) {
            switch (node.type) {
                case EMERGENCY:
                    engine.setBattleNodeType(NodeType.EMERGENCY);
                    break;
                case BOSS:
                    engine.setBattleNodeType(NodeType.BOSS);
                    break;
                default:
                    engine.setBattleNodeType(NodeType.FIGHT);
                    break;
            }
        }
        engine.setRelicManager(new RelicManager(player));
        return engine;
    }

    /**
     * 根据节点类型和当前楼层从 JSON 数据中选取敌人组。
     * <p>
     * 规则：
     * - BOSS 节点：选 isBoss=true 的组
     * - EMERGENCY 节点：优先选非 Boss 组（代表精英/高难度）
     * - FIGHT 节点：选非 Boss 组
     * - 如果当前楼层没有匹配组，fallback 到训练假人
     */
    private List<Enemy> createEnemiesFor(int nodeType) {
        Random encounterRandom = runState.randomFor("enemies:" + nodeKey(activeNode));
        DataLoader loader = new DataLoader();
        int level = currentLayerIdx + 1;
        List<DataLoader.EnemyGroup> groups = loader.loadMonsters(level);

        // 筛选匹配的怪物组。普通战斗不能混入 emergency 专用组。
        List<DataLoader.EnemyGroup> matched = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (!matchesNodeType(group, nodeType)) {
                continue;
            }
            matched.add(group);
        }

        // 如果紧急作战暂时没有专用组，才退回到非 Boss 高 HP 组。
        if (matched.isEmpty() && nodeType == EMERGENCY) {
            for (DataLoader.EnemyGroup group : groups) {
                if (!group.isBoss() && !isEmergencyGroup(group)) {
                    matched.add(group);
                }
            }
        }

        // 紧急作战：优先尝试从同层中挑选精英级或更高 HP 的组
        // 若没有专门的精英标记，则使用所有非 Boss 组
        if (!matched.isEmpty()) {
            DataLoader.EnemyGroup selected;
            if (nodeType == EMERGENCY) {
                // 优先选敌人数量多/HP 高的组作为精英
                selected = matched.get(encounterRandom.nextInt(matched.size()));
                // 尝试偏向选总 HP 更高的
                for (int attempt = 0; attempt < 3; attempt++) {
                    DataLoader.EnemyGroup candidate = matched.get(encounterRandom.nextInt(matched.size()));
                    if (totalHp(candidate) > totalHp(selected)) {
                        selected = candidate;
                    }
                }
            } else {
                selected = matched.get(encounterRandom.nextInt(matched.size()));
            }

            List<Enemy> enemies = new ArrayList<>();
            if (selected.getEnemies() != null) {
                for (DataLoader.EnemyData data : selected.getEnemies()) {
                    enemies.add(data.toEnemy());
                }
            }
            addBabelTowerEnemy(enemies, groups, selected, nodeType, encounterRandom);
            if (!enemies.isEmpty()) return enemies;
        }

        // 跨层 fallback：尝试相邻层
        for (int fallbackLevel : new int[]{level - 1, level + 1}) {
            if (fallbackLevel < 1 || fallbackLevel > 5) continue;
            List<DataLoader.EnemyGroup> fbGroups = loader.loadMonsters(fallbackLevel);
            for (DataLoader.EnemyGroup group : fbGroups) {
                if (!matchesNodeType(group, nodeType)) {
                    continue;
                }
                if (group.getEnemies() != null && !group.getEnemies().isEmpty()) {
                    List<Enemy> enemies = new ArrayList<>();
                    for (DataLoader.EnemyData data : group.getEnemies()) {
                        enemies.add(data.toEnemy());
                    }
                    return enemies;
                }
            }
        }

        if (nodeType == EMERGENCY) {
            for (int fallbackLevel : new int[]{level - 1, level + 1}) {
                if (fallbackLevel < 1 || fallbackLevel > 5) continue;
                List<DataLoader.EnemyGroup> fbGroups = loader.loadMonsters(fallbackLevel);
                for (DataLoader.EnemyGroup group : fbGroups) {
                    if (group.isBoss() || isEmergencyGroup(group)
                            || group.getEnemies() == null || group.getEnemies().isEmpty()) {
                        continue;
                    }
                    List<Enemy> enemies = new ArrayList<>();
                    for (DataLoader.EnemyData data : group.getEnemies()) {
                        enemies.add(data.toEnemy());
                    }
                    return enemies;
                }
            }
        }

        // 最终 fallback：训练假人
        System.out.println("[MapScreen] 楼层 " + level + " 无匹配敌人组，使用训练假人");
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

    /** 计算怪物组的总生命值（用于精英偏好选择） */
    private static int totalHp(DataLoader.EnemyGroup group) {
        if (group.getEnemies() == null) return 0;
        int total = 0;
        for (DataLoader.EnemyData data : group.getEnemies()) {
            total += data.getMaxHp();
        }
        return total;
    }

    private static boolean matchesNodeType(DataLoader.EnemyGroup group, int nodeType) {
        boolean emergency = isEmergencyGroup(group);
        if (nodeType == BOSS) {
            return group.isBoss();
        }
        if (nodeType == EMERGENCY) {
            return !group.isBoss() && emergency;
        }
        return !group.isBoss() && !emergency;
    }

    private static boolean isEmergencyGroup(DataLoader.EnemyGroup group) {
        String id = group.getId() == null ? "" : group.getId().toLowerCase();
        String name = group.getName() == null ? "" : group.getName();
        return id.contains("emergency") || name.contains("紧急");
    }

    private void applyNodeEntryRelics(int nodeType) {
        if (nodeType != FIGHT && nodeType != EMERGENCY && nodeType != BOSS
                && player.hasRelic("relic_oligarch")) {
            player.gainGold(20);
        }
    }

    private void addBabelTowerEnemy(List<Enemy> enemies,
                                    List<DataLoader.EnemyGroup> groups,
                                    DataLoader.EnemyGroup selected,
                                    int nodeType,
                                    Random encounterRandom) {
        if (nodeType != EMERGENCY || !player.hasRelic("relic_babel_tower")
                || groups == null) {
            return;
        }
        List<DataLoader.EnemyData> candidates = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (group == selected || group.isBoss() || group.getEnemies() == null) {
                continue;
            }
            for (DataLoader.EnemyData data : group.getEnemies()) {
                if (data != null) candidates.add(data);
            }
        }
        if (!candidates.isEmpty()) {
            enemies.add(candidates.get(encounterRandom.nextInt(candidates.size())).toEnemy());
        }
    }

    private String randomEventName() {
        List<String> eventNames = new EventHandler().getEventNames();
        return eventNames.get(runState.randomFor("event-name", nodeKey(currentNode))
                .nextInt(eventNames.size()));
    }

    /** 绘制节点之间的连接线 */
    private void drawConnectionLines() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

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

                    shapeRenderer.setColor(0f, 0f, 0f,
                            node.visited || node.accessible || isConnectedToAccessible
                                    ? 0.86f : 0.62f);
                    Vector2 from = connectionPoint(node, next);
                    Vector2 to = connectionPoint(next, node);
                    shapeRenderer.rectLine(
                            from.x, from.y,
                            to.x, to.y,
                            CONNECTION_LINE_WIDTH
                    );
                }
            }
        }

        shapeRenderer.end();
    }

    private Vector2 connectionPoint(MapNode from, MapNode to) {
        float fromX = from.x + scrollX;
        float fromY = from.y + scrollY;
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        if (Math.abs(dx) < 0.001f && Math.abs(dy) < 0.001f) {
            return new Vector2(fromX, fromY);
        }

        float halfW = nodeDrawWidth(from) / 2f + NODE_FRAME_GAP;
        float halfH = nodeDrawHeight(from) / 2f + NODE_FRAME_GAP;
        float tx = Math.abs(dx) < 0.001f ? Float.POSITIVE_INFINITY : halfW / Math.abs(dx);
        float ty = Math.abs(dy) < 0.001f ? Float.POSITIVE_INFINITY : halfH / Math.abs(dy);
        float t = Math.min(tx, ty);
        return new Vector2(fromX + dx * t, fromY + dy * t);
    }

    /** 绘制可选/已访问节点边框。 */
    private void drawNodeFrames() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node == null) continue;
                float width = nodeDrawWidth(node);
                float height = nodeDrawHeight(node);
                float x = node.x + scrollX - width / 2f - NODE_FRAME_GAP;
                float y = node.y + scrollY - height / 2f - NODE_FRAME_GAP;
                float framedWidth = width + NODE_FRAME_GAP * 2f;
                float framedHeight = height + NODE_FRAME_GAP * 2f;
                if (node.accessible) {
                    shapeRenderer.setColor(1f, 0.82f, 0f, 1f);
                    drawFrame(x, y, framedWidth, framedHeight, NODE_FRAME_THICKNESS);
                } else if (node.visited || node == currentNode) {
                    shapeRenderer.setColor(0f, 0f, 0f, 1f);
                    drawFrame(x, y, framedWidth, framedHeight, NODE_FRAME_THICKNESS);
                }
            }
        }
        shapeRenderer.end();
    }

    private void drawFrame(float x, float y, float width, float height, float thickness) {
        shapeRenderer.rect(x, y, width, thickness);
        shapeRenderer.rect(x, y + height - thickness, width, thickness);
        shapeRenderer.rect(x, y, thickness, height);
        shapeRenderer.rect(x + width - thickness, y, thickness, height);
    }

    /** 绘制所有节点 */
    private void drawNodes() {
        MapNode[][] layer = allLayers[currentLayerIdx];
        if (layer == null) return;

        for (int col = 0; col < layer.length; col++) {
            for (int row = 0; row < layer[col].length; row++) {
                MapNode node = layer[col][row];
                if (node == null) continue;

                Texture tex = nodeTexture(node);

                // 根据节点状态设置透明度
                batch.setColor(node.visited ? 0.85f : 1f,
                        node.visited ? 0.85f : 1f,
                        node.visited ? 0.85f : 1f,
                        node.accessible || node.visited ? 1f : 0.82f);

                if (tex != null) {
                    float drawWidth = nodeDrawWidth(node);
                    float drawHeight = nodeDrawHeight(node);
                    batch.draw(tex,
                            node.x + scrollX - drawWidth / 2f,
                            node.y + scrollY - drawHeight / 2f,
                            drawWidth,
                            drawHeight);
                }

                batch.setColor(1, 1, 1, 1);
            }
        }
    }

    private Texture nodeTexture(MapNode node) {
        int texIdx = NODE_TYPE_TO_TEXTURE[node.type];
        if (texIdx >= 1 && texIdx <= 9) {
            return nodeTextures[texIdx];
        }
        return null;
    }

    private float nodeDrawWidth(MapNode node) {
        Texture tex = nodeTexture(node);
        if (tex == null) return NODE_WIDTH;
        float scale = Math.min(NODE_WIDTH / tex.getWidth(), NODE_HEIGHT / tex.getHeight());
        return tex.getWidth() * scale;
    }

    private float nodeDrawHeight(MapNode node) {
        Texture tex = nodeTexture(node);
        if (tex == null) return NODE_HEIGHT;
        float scale = Math.min(NODE_WIDTH / tex.getWidth(), NODE_HEIGHT / tex.getHeight());
        return tex.getHeight() * scale;
    }

    /** 绘制顶部状态栏 */
    private void drawTopBar() {
        Vector2 mouse = toWorld(Gdx.input.getX(), Gdx.input.getY());
        topStatusBar.draw(batch, shapeRenderer, camera, currentLayerStatusText(),
                true, mouse);
    }

    public String currentLayerStatusText() {
        return "第 " + (currentLayerIdx + 1) + " 层  " + LAYER_NAMES[currentLayerIdx];
    }

    private void drawLayerIntro() {
        if (layerIntroTimer <= 0) return;

        float alpha = Math.min(1f, layerIntroTimer / 0.7f);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.55f * alpha);
        shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        shapeRenderer.end();

        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha);
        drawCenteredText(game.getFontForScale(2.2f), "第 " + (currentLayerIdx + 1) + " 层",
                FabricBookGame.SCREEN_HEIGHT / 2f + 40);
        drawCenteredText(game.getFontForScale(1.1f), LAYER_NAMES[currentLayerIdx],
                FabricBookGame.SCREEN_HEIGHT / 2f);
        drawCenteredText(game.getFont(), LAYER_EFFECTS[currentLayerIdx],
                FabricBookGame.SCREEN_HEIGHT / 2f - 42);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private void drawCenteredText(BitmapFont drawFont, String text, float y) {
        glyphLayout.setText(drawFont, text);
        drawFont.draw(batch, text,
                FabricBookGame.SCREEN_WIDTH / 2f - glyphLayout.width / 2f,
                y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}
    @Override
    public void dispose() {
        if (uiStage != null) uiStage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        for (int i = 1; i <= 9; i++) {
            if (nodeTextures[i] != null) nodeTextures[i].dispose();
        }
        if (bgTexture != null) bgTexture.dispose();
    }
}
