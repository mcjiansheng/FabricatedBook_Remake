package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;

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
    private final MapScreen returnMap;
    private Stage stage;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
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
        this(game, player, eventName, null);
    }

    public EventScreen(FabricBookGame game, Player player, String eventName,
                       MapScreen returnMap) {
        this.game = game;
        this.player = player;
        this.eventHandler = new EventHandler();
        this.eventName = eventName;
        this.returnMap = returnMap;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
        this.resolved = false;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);
        shapeRenderer = new ShapeRenderer();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(94, 118, 70, 118);
        stage.addActor(root);

        Table eventText = new Table();
        eventText.top().left();

        Label titleLabel = new Label(eventName, new Label.LabelStyle(game.getFont(), Color.BLACK));
        titleLabel.setFontScale(1.7f);
        eventText.add(titleLabel).left().padBottom(34);
        eventText.row();

        Label description = new Label(eventHandler.getEventDescription(eventName),
                new Label.LabelStyle(game.getFont(), Color.BLACK));
        description.setWrap(true);
        eventText.add(description).width(480).left();

        optionTable = new Table();
        optionTable.top().left();
        renderOptions();

        root.add(eventText).width(520).expandY().top().left().padRight(108);
        root.add(optionTable).width(560).expandY().top().left();
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

            TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
            buttonStyle.font = game.getFont();
            buttonStyle.fontColor = Color.WHITE;
            TextButton btn = new TextButton(cleanLabel(option.label) + "\n" + option.description,
                    buttonStyle);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
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
                    if (result.relicId != null && !result.relicId.isBlank()) {
                        Relic relic = RelicFactory.createById(result.relicId, player);
                        if (relic != null) {
                            new RelicManager(player).addRelic(relic);
                        }
                    }

                    // 显示结果
                    showResult(result.description);
                }
            });
            optionTable.add(btn).width(540).height(96).left().padBottom(28);
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
                game.getFont(), Color.WHITE));
        resultLabel.setWrap(true);
        optionTable.add(resultLabel).width(540).left().padBottom(34);
        optionTable.row();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.getFont();
        buttonStyle.fontColor = Color.WHITE;
        TextButton backBtn = new TextButton("继续前进", buttonStyle);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (returnMap != null) {
                    returnMap.completeCurrentNodeAndReturn();
                } else {
                    game.setScreen(new MapScreen(game, player));
                }
            }
        });
        optionTable.add(backBtn).width(220).height(56).left();
    }

    private String cleanLabel(String label) {
        return label.replaceAll("[^\\p{IsHan}A-Za-z0-9 ]", "").trim();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.78f, 0.78f, 0.78f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawOptionBackplates();

        stage.act(delta);
        stage.draw();
    }

    private void drawOptionBackplates() {
        if (optionTable == null || shapeRenderer == null) {
            return;
        }
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        float x = 118f + 520f + 108f;
        float top = FabricBookGame.SCREEN_HEIGHT - 94f;
        int rows = resolved ? 1 : eventHandler.getOptions(eventName).size();
        for (int i = 0; i < rows; i++) {
            float cardY = top - 96 - i * 124;
            shapeRenderer.rect(x, cardY, 540, resolved ? 150 : 96);
        }
        if (resolved) {
            shapeRenderer.setColor(0.38f, 0.38f, 0.38f, 1f);
            shapeRenderer.rect(x, top - 240, 220, 56);
        }
        shapeRenderer.end();
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
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
