package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.TopStatusBar;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiTheme;
import com.fabricatedbook.view.ui.PotionActionBar;

import java.util.List;
import java.util.Random;

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
    private SpriteBatch batch;
    private BitmapFont font;
    private TopStatusBar topStatusBar;
    private Table optionTable;
    private boolean resolved;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;

    private static final float CONTENT_TOP_PAD = 138f;
    private static final float CONTENT_LEFT_PAD = 72f;
    private static final float CONTENT_RIGHT_PAD = 72f;
    private static final float EVENT_TEXT_WIDTH = 480f;
    private static final float COLUMN_GAP = 48f;
    private static final float OPTION_WIDTH = 528f;
    private static final float OPTION_HEIGHT = 84f;

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
        this(game, player, eventName, returnMap, null);
    }

    public EventScreen(FabricBookGame game, Player player, String eventName,
                       MapScreen returnMap, Random eventRandom) {
        this.game = game;
        this.player = player;
        this.eventHandler = new EventHandler(eventRandom);
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
        camera = (OrthographicCamera) stage.getCamera();
        Gdx.input.setInputProcessor(stage);
        shapeRenderer = new ShapeRenderer();
        batch = game.getBatch();
        font = game.getFont();
        topStatusBar = new TopStatusBar(player, font);

        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(CONTENT_TOP_PAD, CONTENT_LEFT_PAD, 70, CONTENT_RIGHT_PAD);
        stage.addActor(root);

        Table eventText = new Table();
        eventText.top().left();
        eventText.setBackground(UiStyles.panelSurface());
        eventText.pad(32);

        Label titleLabel = new Label(eventName, new Label.LabelStyle(
                game.getFontForScale(1.7f), UiTheme.ACCENT_GOLD));
        eventText.add(titleLabel).left().padBottom(34);
        eventText.row();

        Label description = new Label(eventHandler.getEventDescription(eventName),
                new Label.LabelStyle(game.getFont(), Color.WHITE));
        description.setWrap(true);
        description.setAlignment(Align.left);
        eventText.add(description).width(400).left();

        optionTable = new Table();
        optionTable.top().left();
        optionTable.setBackground(UiStyles.panelSurface());
        optionTable.pad(24);
        renderOptions();

        root.add(eventText).width(EVENT_TEXT_WIDTH).top().left().padRight(COLUMN_GAP);
        root.add(optionTable).width(OPTION_WIDTH).top().left();

        PotionActionBar potions = new PotionActionBar(stage, game, player, false, null, null);
        potions.setPosition(24, FabricBookGame.SCREEN_HEIGHT - 128);
        stage.addActor(potions);
    }

    /**
     * 渲染事件选项。
     */
    private void renderOptions() {
        optionTable.clear();
        List<EventHandler.EventOption> options = eventHandler.getOptions(eventName, player);

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            EventHandler.EventOption option = options.get(i);

            TextButton.TextButtonStyle buttonStyle = UiStyles.buttonStyle(game);
            TextButton btn = new TextButton(cleanLabel(option.label) + "\n" + option.description,
                    buttonStyle);
            btn.getLabel().setAlignment(Align.left);
            btn.getLabelCell().padLeft(28).padRight(24);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (resolved) return;
                    resolved = true;

                    EventHandler.EventResult result =
                            eventHandler.executeEvent(eventName, index, player);

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
                    game.autosaveCurrentRun();
                    if (showEndingIfNeeded(result)) {
                        return;
                    }
                    showResult(result.description);
                }
            });
            optionTable.add(btn).width(480).height(OPTION_HEIGHT)
                    .left().padBottom(28);
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
        resultLabel.setAlignment(Align.left);
        optionTable.add(resultLabel).width(480).left().padBottom(34);
        optionTable.row();

        TextButton.TextButtonStyle buttonStyle = UiStyles.buttonStyle(game);
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
        optionTable.add(backBtn).width(260).height(56).left();
    }

    private boolean showEndingIfNeeded(EventHandler.EventResult result) {
        if (result.outcome == null || result.outcome.isBlank()) {
            return false;
        }
        EndingScreen.EndingType endingType = switch (result.outcome) {
            case "ENDING_INTERRUPTED" -> EndingScreen.EndingType.INTERRUPTED;
            case "ENDING_HIDDEN" -> EndingScreen.EndingType.HIDDEN;
            default -> EndingScreen.EndingType.NORMAL;
        };
        game.setScreen(new EndingScreen(game, endingType));
        return true;
    }

    private String cleanLabel(String label) {
        return label.replaceAll("[^\\p{IsHan}A-Za-z0-9 ]", "").trim();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
        }
        Gdx.gl.glClearColor(UiTheme.MAP_BACKGROUND.r, UiTheme.MAP_BACKGROUND.g,
                UiTheme.MAP_BACKGROUND.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawTopBar();

        stage.act(delta);
        stage.draw();
    }

    private void drawTopBar() {
        String layerText = returnMap != null ? returnMap.currentLayerStatusText() : "";
        topStatusBar.draw(batch, shapeRenderer, camera, layerText,
                false, new Vector2());
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() { game.autosaveCurrentRun(); }
    @Override public void resume() {}
    @Override public void hide() {
        game.autosaveCurrentRun();
        Gdx.input.setInputProcessor(null);
    }
    @Override public void dispose() {
        stage.dispose();
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    private void toggleEscapeMenu() {
        if (escapeMenu != null && escapeMenu.hasParent()) {
            escapeMenu.remove();
            escapeMenu = null;
            return;
        }
        escapeMenu = EscapeMenu.show(stage, game, () -> escapeMenu = null);
    }
}
