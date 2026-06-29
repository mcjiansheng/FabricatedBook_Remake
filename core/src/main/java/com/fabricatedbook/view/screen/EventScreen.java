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
import com.fabricatedbook.core.event.EventRewardResolver;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiTheme;
import com.fabricatedbook.view.ui.GameHud;
import com.fabricatedbook.view.ui.UiLayout;

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
    private final Random eventRandom;
    private final String eventName;
    private final MapScreen returnMap;
    private final String entryMessage;
    private Stage stage;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private Table optionTable;
    private boolean resolved;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;

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
        this(game, player, eventName, returnMap, eventRandom, null);
    }

    public EventScreen(FabricBookGame game, Player player, String eventName,
                       MapScreen returnMap, Random eventRandom,
                       String entryMessage) {
        this.game = game;
        this.player = player;
        this.eventRandom = eventRandom == null ? new Random() : eventRandom;
        this.eventHandler = new EventHandler(this.eventRandom);
        this.eventName = eventName;
        this.returnMap = returnMap;
        this.entryMessage = entryMessage;
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

        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(UiLayout.PAGE_TOP_AFTER_HUD, UiLayout.PAGE_SIDE_PADDING,
                UiLayout.PANEL_PADDING, UiLayout.PAGE_SIDE_PADDING);
        stage.addActor(root);

        Table eventText = new Table();
        eventText.top().left();
        eventText.setBackground(UiStyles.panelSurface());
        eventText.pad(UiLayout.PANEL_PADDING);

        Label titleLabel = new Label(eventName, new Label.LabelStyle(
                game.getFontForScale(1.7f), UiTheme.ACCENT_GOLD));
        eventText.add(titleLabel).left().padBottom(34);
        eventText.row();

        Label description = new Label(eventDescriptionText(),
                new Label.LabelStyle(game.getFont(), Color.WHITE));
        description.setWrap(true);
        description.setAlignment(Align.left);
        eventText.add(description).width(400).left();

        optionTable = new Table();
        optionTable.top().left();
        optionTable.setBackground(UiStyles.panelSurface());
        optionTable.pad(UiLayout.PANEL_PADDING);
        renderOptions();

        root.add(eventText).width(EVENT_TEXT_WIDTH).top().left().padRight(UiLayout.SECTION_GAP * 2f);
        root.add(optionTable).width(OPTION_WIDTH).top().left();

        new GameHud(stage, game, player,
                () -> returnMap != null ? returnMap.currentLayerStatusText() : "第" + player.getCurrentFloor() + "层",
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.CARDS)),
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.RELICS)),
                false, null, null);
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
                    if (result.fullHeal) {
                        player.heal(player.getMaxHp());
                    } else if (result.hpChange != 0) {
                        if (result.hpChange > 0) {
                            player.heal(result.hpChange);
                        } else {
                            player.takeDamage(-result.hpChange);
                        }
                    }
                    EventRewardResolver.EventReward eventReward =
                            EventRewardResolver.applyRewards(result, player,
                                    EventScreen.this.eventRandom);
                    markNodeProgressCommitted();

                    // 显示结果
                    game.autosaveCurrentRun();
                    if (showEndingIfNeeded(result)) {
                        return;
                    }
                    showResult(result, eventReward);
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
     * @param result 已结算的事件结果
     * @param reward 本次应用的事件奖励摘要。
     */
    private void showResult(EventHandler.EventResult result,
                            EventRewardResolver.EventReward reward) {
        optionTable.clear();

        Label resultLabel = new Label(result.description, new Label.LabelStyle(
                game.getFont(), Color.WHITE));
        resultLabel.setWrap(true);
        resultLabel.setAlignment(Align.left);
        optionTable.add(resultLabel).width(480).left().padBottom(20);
        optionTable.row();

        String summary = resultSummary(result, reward);
        if (!summary.isBlank()) {
            Label summaryLabel = new Label(summary, new Label.LabelStyle(game.getFont(), UiTheme.ACCENT_GOLD));
            summaryLabel.setWrap(true);
            summaryLabel.setAlignment(Align.left);
            optionTable.add(summaryLabel).width(480).left().padBottom(28);
            optionTable.row();
        }

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

    private String eventDescriptionText() {
        String description = eventHandler.getEventDescription(eventName);
        if (entryMessage == null || entryMessage.isBlank()) {
            return description;
        }
        return entryMessage + "\n\n" + description;
    }

    private String resultSummary(EventHandler.EventResult result,
                                 EventRewardResolver.EventReward reward) {
        StringBuilder summary = new StringBuilder();
        if (result.goldChange != 0) {
            summary.append(result.goldChange > 0 ? "金币 +" : "金币 ")
                    .append(result.goldChange);
        }
        if (result.fullHeal) {
            if (summary.length() > 0) summary.append("   ");
            summary.append("生命回满");
        } else if (result.hpChange != 0) {
            if (summary.length() > 0) summary.append("   ");
            summary.append(result.hpChange > 0
                    ? "生命 +" + result.hpChange
                    : "生命 " + result.hpChange);
        }
        if (reward != null && reward.getRelicName() != null
                && !reward.getRelicName().isBlank()) {
            if (summary.length() > 0) summary.append("   ");
            summary.append("获得藏品：").append(reward.getRelicName());
        }
        if (reward != null && !reward.cardNames().isBlank()) {
            if (summary.length() > 0) summary.append("   ");
            summary.append("获得卡牌：").append(reward.cardNames());
        }
        if (reward != null && reward.getUnresolvedSpecialRewardId() != null
                && !reward.getUnresolvedSpecialRewardId().isBlank()) {
            if (summary.length() > 0) summary.append("   ");
            summary.append("特殊奖励待接入：")
                    .append(reward.getUnresolvedSpecialRewardId());
        }
        return summary.toString();
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

    private void markNodeProgressCommitted() {
        if (game.getCurrentRun() != null) {
            game.getCurrentRun().markActiveNodeProgressCommitted();
        }
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

        stage.act(delta);
        stage.draw();
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
