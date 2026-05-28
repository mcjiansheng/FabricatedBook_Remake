package com.fabricatedbook.view;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;
import com.fabricatedbook.view.screen.TitleScreen;

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

    /** 数据加载器 */
    private DataLoader dataLoader;

    /** 存档管理器 */
    private SaveManager saveManager;

    /** 屏幕宽度 */
    public static final int SCREEN_WIDTH = 1280;

    /** 屏幕高度 */
    public static final int SCREEN_HEIGHT = 720;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 加载中文字体（从原项目复制的 ys_zt.ttf）
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.classpath("ys_zt.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 18;
        // 包含中英文全部字符（从字体文件读取所有可用字符）
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
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
                + "，。、；：！？（）【】《》“”‘’—…·～/=+*#@&%￥①②③④⑤";
        font = generator.generateFont(parameter);
        generator.dispose();
        dataLoader = new DataLoader("data/");
        saveManager = new SaveManager();

        // 初始化卡牌池（确保战士卡牌已注册）
        CardPool.getCardsByProfession("warrior");

        // 切换到标题屏幕
        setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }

    // ====== Getter ======

    public SpriteBatch getBatch() { return batch; }
    public BitmapFont getFont() { return font; }
    public DataLoader getDataLoader() { return dataLoader; }
    public SaveManager getSaveManager() { return saveManager; }
}
