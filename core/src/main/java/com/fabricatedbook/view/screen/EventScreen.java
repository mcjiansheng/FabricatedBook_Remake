package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.FabricBookGame;

import java.util.List;

/**
 * EventScreen — 事件画面
 * <p>
 * 显示随机事件的描述文本和选项列表。
 * 玩家选择选项后显示结果。
 * <p>
 * 引用方：MapScreen（进入不期而遇节点时切换）
 */
public class EventScreen implements Screen {

    private final FabricBookGame game;
    private final Player player;
    private final EventHandler eventHandler;
    private final String eventName;
    private Stage stage;
    private OrthographicCamera camera;
    private Label descriptionLabel;
    private Table optionTable;
    private boolean resolved;

    /**
     * 构造事件画面。
     *
     * @param game          游戏主类
     * @param player        玩家实体
     * @param eventName     事件名称
     */
    public EventScreen(FabricBookGame game, Player player, String eventName) {
        this.game = game;
        this.player = player;
        this.eventHandler = new EventHandler();
        this.eventName = eventName;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
        this.resolved = false;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // 事件标题
        Label titleLabel = new Label("【" + eventName + "】",
                new Label.LabelStyle(game.getFont(),
                        com.badlogic.gdx.graphics.Color.GOLD));
        titleLabel.setFontScale(1.8f);
        titleLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 100,
                FabricBookGame.SCREEN_HEIGHT - 80);
        stage.addActor(titleLabel);

        // 选项列表
        optionTable = new Table();
        optionTable.setFillParent(true);
        optionTable.center();
        renderOptions();
        stage.addActor(optionTable);
    }

    /**
     * 渲染事件选项。
     */
    private void renderOptions() {
        optionTable.clear();
        List<EventHandler.EventOption> options = eventHandler.getOptions(eventName);

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            EventHandler.EventOption option = options.get(i);

            TextButton btn = new TextButton(option.label + " - " + option.description,
                    new TextButton.TextButtonStyle());
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y) {
                    if (resolved) return;
                    resolved = true;

                    EventHandler.EventResult result =
                            eventHandler.executeEvent(eventName, index);

                    // 应用结果
                    if (result.goldChange != 0) {
                        if (result.goldChange > 0) {
                            player.gainGold(result.goldChange);
                        } else {
                            player.spendGold(-result.goldChange);
                        }
                    }
                    if (result.hpChange != 0) {
                        if (result.hpChange > 0 && result.hpChange < 9999) {
                            player.heal(result.hpChange);
                        } else if (result.hpChange > 0) {
                            player.heal(player.getMaxHp()); // 回满
                        } else {
                            player.takeDamage(-result.hpChange);
                        }
                    }

                    // 显示结果
                    showResult(result.description);
                }
            });
            optionTable.add(btn).width(600).height(50).pad(8);
            optionTable.row();
        }
    }

    /**
     * 显示事件结果并添加返回按钮。
     *
     * @param resultDescription 结果描述
     */
    private void showResult(String resultDescription) {
        optionTable.clear();

        Label resultLabel = new Label(resultDescription, new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        resultLabel.setWrap(true);
        optionTable.add(resultLabel).width(600).pad(20);
        optionTable.row();

        TextButton backBtn = new TextButton("继续前进",
                new TextButton.TextButtonStyle());
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.setScreen(new MapScreen(game, player));
            }
        });
        optionTable.add(backBtn).width(200).height(50).pad(20);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.12f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }
    @Override public void dispose() {
        stage.dispose();
    }
}
