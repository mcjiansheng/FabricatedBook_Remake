package com.fabricatedbook.view;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;
import com.fabricatedbook.view.screen.BattleScreen;
import com.fabricatedbook.view.screen.EventScreen;
import com.fabricatedbook.view.screen.FontDebugScreen;
import com.fabricatedbook.view.screen.InventoryScreen;
import com.fabricatedbook.view.screen.MapScreen;
import com.fabricatedbook.view.screen.ShopScreen;
import com.fabricatedbook.view.screen.TitleScreen;
import com.fabricatedbook.view.ui.UiStyles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FabricBookGame — LibGDX Game 入口类
 * <p>
 * LibGDX 应用程序入口，管理全局资源（SpriteBatch、BitmapFont）
 * 和屏幕切换。继承自 com.badlogic.gdx.Game。
 * <p>
 * 引用方：DesktopLauncher（创建实例）、各 Screen 子类（切换屏幕）
 */
public class FabricBookGame extends Game {

    /** 全局 SpriteBatch */
    private SpriteBatch batch;

    /** 全局字体 */
    private BitmapFont font;

    /** 按逻辑字号缓存的字体贴图。 */
    private Map<Integer, BitmapFont> fontsBySize;

    private FreeTypeFontGenerator fontGenerator;

    /** 数据加载器 */
    private DataLoader dataLoader;

    /** 存档管理器 */
    private SaveManager saveManager;

    private GameRunState currentRun;

    /** 屏幕宽度 */
    public static final int SCREEN_WIDTH = 1280;

    /** 屏幕高度 */
    public static final int SCREEN_HEIGHT = 720;

    /** 最小窗口宽度。 */
    public static final int MIN_WINDOW_WIDTH = 960;

    /** 最小窗口高度。 */
    public static final int MIN_WINDOW_HEIGHT = 540;

    /** 推荐窗口最大宽度。 */
    public static final int MAX_WINDOW_WIDTH = 3840;

    /** 推荐窗口最大高度。 */
    public static final int MAX_WINDOW_HEIGHT = 2160;

    /** 全局正文字体字号。 */
    public static final int BASE_FONT_SIZE = 18;

    private int lastWindowedWidth = SCREEN_WIDTH;
    private int lastWindowedHeight = SCREEN_HEIGHT;
    private boolean fullscreen;
    private boolean borderless;
    private final FrontendDebugConfig debugConfig;

    public FabricBookGame() {
        this(FrontendDebugConfig.title());
    }

    public FabricBookGame(String[] args) {
        this(FrontendDebugConfig.parse(args));
    }

    public FabricBookGame(FrontendDebugConfig debugConfig) {
        this.debugConfig = debugConfig == null ? FrontendDebugConfig.title() : debugConfig;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        fontsBySize = new HashMap<>();

        // 加载中文字体（从原项目复制的 ys_zt.ttf）
        fontGenerator = new FreeTypeFontGenerator(
                Gdx.files.classpath("ys_zt.ttf"));
        font = getFontSize(BASE_FONT_SIZE);
        dataLoader = new DataLoader("data/");
        saveManager = new SaveManager();

        // 初始化卡牌池（确保战士卡牌已注册）
        CardPool.getCardsByProfession("warrior");

        setInitialScreen();
    }

    @Override
    public void render() {
        handleDisplayShortcuts();
        super.render();
    }

    @Override
    public void dispose() {
        autosaveCurrentRun();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        UiStyles.dispose();
        batch.dispose();
        for (BitmapFont cachedFont : fontsBySize.values()) {
            cachedFont.dispose();
        }
        fontsBySize.clear();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
    }

    // ====== Getter ======

    public SpriteBatch getBatch() { return batch; }
    public BitmapFont getFont() { return font; }
    public BitmapFont getFontForScale(float logicalScale) {
        return getFontSize(Math.max(1, Math.round(BASE_FONT_SIZE * logicalScale)));
    }
    public BitmapFont getFontSize(int size) {
        return fontsBySize.computeIfAbsent(size, this::generateFont);
    }
    public DataLoader getDataLoader() { return dataLoader; }
    public SaveManager getSaveManager() { return saveManager; }
    public GameRunState getCurrentRun() { return currentRun; }
    public void setCurrentRun(GameRunState currentRun) { this.currentRun = currentRun; }

    public void autosaveCurrentRun() {
        if (saveManager != null && currentRun != null) {
            saveManager.saveRun(currentRun);
        }
    }

    public void abandonCurrentRun() {
        currentRun = null;
        if (saveManager != null) {
            saveManager.deleteSave();
        }
    }

    public void applyFontScale(float logicalScale) {
        font.getData().setScale(logicalScale);
    }

    public static float uiFontScale(float logicalScale) {
        return logicalScale;
    }

    private BitmapFont generateFont(int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.genMipMaps = false;
        parameter.minFilter = TextureFilter.Linear;
        parameter.magFilter = TextureFilter.Linear;
        parameter.incremental = true;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        return fontGenerator.generateFont(parameter);
    }

    private void setInitialScreen() {
        Player player = createDebugPlayer();
        switch (debugConfig.getScreenKind()) {
            case FONT_DEBUG:
                setScreen(new FontDebugScreen(this));
                break;
            case MAP:
                setScreen(new MapScreen(this,
                        new GameRunState(System.currentTimeMillis(), player),
                        debugConfig.getLayer()));
                break;
            case BATTLE:
                CombatEngine engine = new CombatEngine();
                engine.setRelicManager(new RelicManager(player));
                setScreen(new BattleScreen(this, engine, player, createDebugEnemies()));
                break;
            case REWARD_POTION:
                CombatEngine rewardEngine = new CombatEngine();
                rewardEngine.setRelicManager(new RelicManager(player));
                BattleScreen rewardScreen = new BattleScreen(this, rewardEngine, player, createDebugEnemies());
                setScreen(rewardScreen);
                rewardScreen.showDebugPotionReward();
                break;
            case SHOP:
                ShopManager shopManager = new ShopManager(player, new RelicManager(player));
                shopManager.generateItems();
                setScreen(new ShopScreen(this, player, shopManager));
                break;
            case EVENT:
                String eventName = debugConfig.getEventName();
                if (eventName == null || eventName.isBlank()) {
                    eventName = "投资";
                }
                setScreen(new EventScreen(this, player, eventName));
                break;
            case INVENTORY:
                setScreen(new InventoryScreen(this, player, null));
                break;
            case TITLE:
            default:
                setScreen(new TitleScreen(this));
                break;
        }
    }

    private Player createDebugPlayer() {
        Player player = new Player("debug-player", "调试战士", Profession.WARRIOR);
        player.setGold(99);
        // 前端调试场景必须包含可展示、可移除的牌组，避免商店/总览只覆盖空状态。
        List<com.fabricatedbook.core.card.Card> cards = CardPool.getCardsByProfession("warrior");
        for (int i = 0; i < Math.min(10, cards.size()); i++) {
            player.getDrawPile().add(CardFactory.createFromTemplate(cards.get(i)));
        }
        player.setCardCount(player.getDrawPile().size());
        List<Potion> potions = new DataLoader().loadPotions();
        for (int i = 0; i < potions.size() && i < 3; i++) {
            player.addPotion(potions.get(i).copy());
        }
        return player;
    }

    private List<Enemy> createDebugEnemies() {
        List<DataLoader.EnemyGroup> groups = new DataLoader().loadMonsters(1);
        for (DataLoader.EnemyGroup group : groups) {
            if (group.isBoss() || group.getEnemies() == null || group.getEnemies().isEmpty()) {
                continue;
            }
            return group.getEnemies().stream()
                    .map(DataLoader.EnemyData::toEnemy)
                    .toList();
        }
        return List.of(EntityFactory.createSimpleEnemy("debug_enemy", "调试敌人", 35));
    }

    private void handleDisplayShortcuts() {
        boolean altEnter = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
        altEnter = altEnter && Gdx.input.isKeyJustPressed(Input.Keys.ENTER);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11) || altEnter) {
            toggleFullscreen();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            toggleBorderless();
        }
    }

    @Override
    public void pause() {
        autosaveCurrentRun();
        super.pause();
    }

    private void toggleFullscreen() {
        if (fullscreen) {
            fullscreen = false;
            Gdx.graphics.setUndecorated(borderless);
            Gdx.graphics.setWindowedMode(lastWindowedWidth, lastWindowedHeight);
            return;
        }

        lastWindowedWidth = Math.max(MIN_WINDOW_WIDTH, Gdx.graphics.getWidth());
        lastWindowedHeight = Math.max(MIN_WINDOW_HEIGHT, Gdx.graphics.getHeight());
        fullscreen = true;
        Gdx.graphics.setUndecorated(false);
        DisplayMode mode = Gdx.graphics.getDisplayMode();
        Gdx.graphics.setFullscreenMode(mode);
    }

    private void toggleBorderless() {
        if (fullscreen) {
            toggleFullscreen();
        }
        borderless = !borderless;
        Gdx.graphics.setUndecorated(borderless);
        Gdx.graphics.setWindowedMode(
                clampWindowWidth(Gdx.graphics.getWidth()),
                clampWindowHeight(Gdx.graphics.getHeight()));
    }

    private int clampWindowWidth(int width) {
        return Math.max(MIN_WINDOW_WIDTH, Math.min(MAX_WINDOW_WIDTH, width));
    }

    private int clampWindowHeight(int height) {
        return Math.max(MIN_WINDOW_HEIGHT, Math.min(MAX_WINDOW_HEIGHT, height));
    }

}
