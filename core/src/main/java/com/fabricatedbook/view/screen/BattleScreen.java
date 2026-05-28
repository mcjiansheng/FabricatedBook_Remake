package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.actor.CardActor;
import com.fabricatedbook.view.actor.EnemyActor;
import com.fabricatedbook.view.actor.PlayerActor;
import com.fabricatedbook.view.ui.HandPanel;
import com.fabricatedbook.view.ui.EnergyBar;
import com.fabricatedbook.view.ui.BuffBar;

import java.util.ArrayList;
import java.util.List;

/**
 * BattleScreen — 战斗画面
 * <p>
 * 显示战斗场景，包括玩家手牌、敌人、格挡/Buff/能量等状态。
 * 通过 ViewNotifier 接口接收战斗引擎的事件通知。
 * 实现用户交互：选择目标、使用卡牌、结束回合等。
 * <p>
 * 引用方：MapScreen（进入战斗节点时切换）、CombatEngine（战斗结算）
 */
public class BattleScreen implements Screen, ViewNotifier {

    private final FabricBookGame game;
    private final CombatEngine combatEngine;
    private final Player player;
    private final List<Enemy> enemies;

    private Stage stage;
    private OrthographicCamera camera;

    // UI 组件
    private HandPanel handPanel;
    private EnergyBar energyBar;
    private BuffBar buffBar;
    private PlayerActor playerActor;
    private List<EnemyActor> enemyActors;
    private Label statusLabel;
    private Label turnLabel;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    /**
     * 构造战斗画面。
     *
     * @param game         游戏主类
     * @param combatEngine 战斗引擎
     * @param player       玩家实体
     * @param enemies      敌人列表
     */
    public BattleScreen(FabricBookGame game, CombatEngine combatEngine,
                        Player player, List<Enemy> enemies) {
        this.game = game;
        this.combatEngine = combatEngine;
        this.player = player;
        this.enemies = enemies;
        this.enemyActors = new ArrayList<>();

        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        font = game.getFont();
        shapeRenderer = new ShapeRenderer();

        // 玩家角色
        playerActor = new PlayerActor(player, font, shapeRenderer);
        playerActor.setPosition(100, 100);
        stage.addActor(playerActor);

        // 敌人角色
        float enemyX = FabricBookGame.SCREEN_WIDTH - 400;
        float enemyY = 300;
        for (int i = 0; i < enemies.size(); i++) {
            EnemyActor actor = new EnemyActor(enemies.get(i), font, shapeRenderer);
            actor.setPosition(enemyX, enemyY - i * 180);
            stage.addActor(actor);
            enemyActors.add(actor);
        }

        // 手牌面板（底部）
        handPanel = new HandPanel(game, combatEngine, this);
        handPanel.setPosition(0, 0);
        stage.addActor(handPanel);

        // 能量条
        energyBar = new EnergyBar(player);
        energyBar.setPosition(FabricBookGame.SCREEN_WIDTH - 150,
                FabricBookGame.SCREEN_HEIGHT - 50);
        stage.addActor(energyBar);

        // Buff 栏
        buffBar = new BuffBar(player);
        buffBar.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 50);
        stage.addActor(buffBar);

        // 状态信息
        statusLabel = new Label("", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        statusLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 100,
                FabricBookGame.SCREEN_HEIGHT - 100);
        stage.addActor(statusLabel);

        // 回合信息
        turnLabel = new Label("回合 1", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        turnLabel.setFontScale(1.5f);
        turnLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 50,
                FabricBookGame.SCREEN_HEIGHT - 50);
        stage.addActor(turnLabel);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // UI 更新
        energyBar.update();
        buffBar.update();
        handPanel.update();

        stage.act(delta);
        stage.draw();
    }

    // ==================== ViewNotifier 实现 ====================

    @Override
    public void onBattleStart(AbstractEntity playerEntity,
                              List<AbstractEntity> enemyEntities) {
        statusLabel.setText("战斗开始！");
    }

    @Override
    public void onActionExecuted(CombatAction action) {
        statusLabel.setText(action.getDescription());
    }

    @Override
    public void onBattleEnd(boolean victory, String reward) {
        if (victory) {
            statusLabel.setText("胜利！" + (reward != null ? reward : ""));
        } else {
            statusLabel.setText("失败...");
        }
    }

    @Override
    public void onTurnStart(int turnNumber) {
        turnLabel.setText("回合 " + turnNumber);
        energyBar.update();
        buffBar.update();
    }

    @Override
    public void onCardPlayed(CombatAction action) {
        statusLabel.setText("使用卡牌: " + action.getDescription());
    }

    // ====== 生命周期 ======

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }
    @Override
    public void dispose() {
        stage.dispose();
    }

    // ====== Getter ======

    public CombatEngine getCombatEngine() { return combatEngine; }
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
}
