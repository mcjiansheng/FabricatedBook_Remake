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
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;
import com.fabricatedbook.view.screen.BattleScreen;
import com.fabricatedbook.view.screen.EventScreen;
import com.fabricatedbook.view.screen.FontDebugScreen;
import com.fabricatedbook.view.screen.MapScreen;
import com.fabricatedbook.view.screen.ShopScreen;
import com.fabricatedbook.view.screen.TitleScreen;

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
    private String fontCharacters;

    /** 数据加载器 */
    private DataLoader dataLoader;

    /** 存档管理器 */
    private SaveManager saveManager;

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
        // 包含中英文全部字符（从字体文件读取所有可用字符）
        fontCharacters = buildFontCharacters(FreeTypeFontGenerator.DEFAULT_CHARS
                + "的一是不了人我在有他这为之大小个中上国到来时出可"
                + "以发会子得要于对生下也而就年过后作里用道行所"
                + "然家种事成方多如学日关前回到长又月入进同面都各"
                + "当使因点只从其主没理心全问开业重物体两新军力外"
                + "者政自分经正名部民组场相与能天法品公高地已安"
                + "路线让图解始命通据车量受很建设声文更东"
                + "单击开始游戏继续退出当前总第层剩余击败获得"
                + "使用掷骰回合结束选择战斗胜利失败死亡回复"
                + "生命值伤害格挡能量金币力量抗性坚强脆弱易碎"
                + "虚弱中毒凋零眩晕装甲不死卡牌效果敌人怪物"
                + "攻击防御技能装备稀有史诗传说神话普通基础"
                + "角色选择战士法师女巫确认返回生命金币药水"
                + "荒野森林诡异秘林迷雾高塔探索者安全屋商店"
                + "事件奖励不期而遇命运抉择精英首领训练假人"
                + "点击可进入拖动地图第层开始当前层效果无"
                + "继续前进离开商店胜利返回地图失败重新开始"
                + "意图多段诅咒强化"
                + "未发现额外物品跳过藏品掉落战斗失败"
                + "虚妄之书Fabricated Book"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                + "字体调试前端调试命令行标题商店事件不同层地图"
                + "红绿蓝黄白黑灰紫青橙粉符号图片ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "，。、；：！？（）【】《》“”‘’—…·～/=+*#@&%￥①②③④⑤");
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
        batch.dispose();
        for (BitmapFont cachedFont : fontsBySize.values()) {
            cachedFont.dispose();
        }
        fontsBySize.clear();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
        if (getScreen() != null) {
            getScreen().dispose();
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
        parameter.characters = fontCharacters;
        return fontGenerator.generateFont(parameter);
    }

    private void setInitialScreen() {
        Player player = createDebugPlayer();
        switch (debugConfig.getScreenKind()) {
            case FONT_DEBUG:
                setScreen(new FontDebugScreen(this));
                break;
            case MAP:
                setScreen(new MapScreen(this, player, debugConfig.getLayer()));
                break;
            case BATTLE:
                CombatEngine engine = new CombatEngine();
                engine.setRelicManager(new RelicManager(player));
                setScreen(new BattleScreen(this, engine, player, createDebugEnemies()));
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
            case TITLE:
            default:
                setScreen(new TitleScreen(this));
                break;
        }
    }

    private Player createDebugPlayer() {
        Player player = new Player("debug-player", "调试战士", Profession.WARRIOR);
        player.setGold(99);
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

    private String buildFontCharacters(String baseCharacters) {
        StringBuilder builder = new StringBuilder(baseCharacters);
        appendResourceText(builder, "data/cards/warrior.json");
        appendResourceText(builder, "data/relics.json");
        appendResourceText(builder, "data/potions.json");
        appendResourceText(builder, "data/events.json");
        appendResourceText(builder, "data/maps/levels.json");
        for (int i = 1; i <= 5; i++) {
            appendResourceText(builder, "data/monsters/level" + i + ".json");
        }
        String raw = builder.toString();
        StringBuilder unique = new StringBuilder();
        java.util.HashSet<Character> seen = new java.util.HashSet<>();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (seen.add(ch)) {
                unique.append(ch);
            }
        }
        return unique.toString();
    }

    private void appendResourceText(StringBuilder builder, String path) {
        try {
            builder.append(Gdx.files.classpath(path).readString("UTF-8"));
        } catch (Exception ignored) {
            // Font generation should not fail just because an optional data file is absent.
        }
    }
}
