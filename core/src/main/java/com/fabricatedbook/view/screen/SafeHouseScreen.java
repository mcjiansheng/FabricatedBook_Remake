package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.GameHud;
import com.fabricatedbook.view.ui.UiLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * SafeHouseScreen — 安全屋节点。
 * <p>
 * 提供休息、治疗和卡牌升级。升级直接作用于玩家当前牌组中的卡牌实例。
 */
public class SafeHouseScreen implements Screen {

    private final FabricBookGame game;
    private final Player player;
    private final MapScreen returnMap;
    private Stage stage;
    private OrthographicCamera camera;
    private Table content;
    private Label statusLabel;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;

    public SafeHouseScreen(FabricBookGame game, Player player, MapScreen returnMap) {
        this.game = game;
        this.player = player;
        this.returnMap = returnMap;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        content = new Table();
        content.setBackground(UiStyles.panelSurface());
        content.pad(UiLayout.PANEL_PADDING);
        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(UiLayout.PAGE_TOP_AFTER_HUD);
        root.add(content).width(880).height(560).top();
        stage.addActor(root);
        new GameHud(stage, game, player,
                () -> returnMap != null ? returnMap.currentLayerStatusText() : "第" + player.getCurrentFloor() + "层",
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.CARDS)),
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.RELICS)),
                false, null, null);
        renderMainOptions();
    }

    private void renderMainOptions() {
        content.clear();
        Label title = new Label("安全屋", new Label.LabelStyle(
                game.getFontForScale(2.0f), Color.GOLD));
        content.add(title).padBottom(26).row();

        statusLabel = new Label("选择一项补给。", new Label.LabelStyle(
                game.getFont(), Color.LIGHT_GRAY));
        content.add(statusLabel).width(760).padBottom(24).row();

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        addOption("好好休息", "回复 20% 最大生命值", style, () -> {
            int amount = Math.max(1, Math.round(player.getMaxHp() * 0.2f));
            int healed = player.heal(amount);
            finish("休息完成，回复 " + healed + " 点生命值。");
        });
        addOption("进行治疗", "花费 50% 金币，恢复全部生命值", style, () -> {
            int cost = Math.max(0, player.getGold() / 2);
            player.spendGold(cost);
            int healed = player.heal(player.getMaxHp());
            finish("治疗完成，花费 " + cost + " 金币，回复 " + healed + " 点生命值。");
        });
        addOption("强化自身", "选择一张未升级的牌，将其升级", style,
                this::renderUpgradeOptions);
        addLeaveButton(style);
    }

    private void addOption(String name, String description,
                           TextButton.TextButtonStyle style, Runnable action) {
        TextButton button = new TextButton(name + "  " + description, style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                action.run();
            }
        });
        content.add(button).width(620).height(56).padBottom(14).row();
    }

    private void renderUpgradeOptions() {
        content.clear();
        Label title = new Label("强化自身", new Label.LabelStyle(
                game.getFontForScale(1.8f), Color.GOLD));
        content.add(title).padBottom(20).row();

        List<Card> upgradeable = upgradeableCards();
        if (upgradeable.isEmpty()) {
            Label empty = new Label("当前牌组没有可升级卡牌。", new Label.LabelStyle(
                    game.getFont(), Color.LIGHT_GRAY));
            content.add(empty).padBottom(24).row();
            addLeaveButton(UiStyles.buttonStyle(game));
            return;
        }

        Table cardTable = new Table();
        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        for (Card card : upgradeable) {
            TextButton button = new TextButton(card.getName() + "  用 "
                    + (card.getCost() < 0 ? "X" : card.getCost())
                    + "  " + card.getDescription(), style);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y) {
                    String before = card.getName();
                    if (card.upgrade()) {
                        finish(before + " 已升级为 " + card.getName() + "。");
                    }
                }
            });
            cardTable.add(button).width(760).height(54).padBottom(10).row();
        }

        ScrollPane scrollPane = new ScrollPane(cardTable);
        content.add(scrollPane).width(820).height(430).padBottom(18).row();
        TextButton back = new TextButton("返回", style);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                renderMainOptions();
            }
        });
        content.add(back).width(160).height(48).row();
    }

    private List<Card> upgradeableCards() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(player.getDrawPile());
        cards.addAll(player.getHand());
        cards.addAll(player.getDiscardPile());
        cards.addAll(player.getExhaustPile());
        return cards.stream().filter(Card::canUpgrade).toList();
    }

    private void finish(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        game.autosaveCurrentRun();
        if (returnMap != null) {
            returnMap.completeCurrentNodeAndReturn();
        } else {
            game.setScreen(new MapScreen(game, player));
        }
    }

    private void addLeaveButton(TextButton.TextButtonStyle style) {
        TextButton leave = new TextButton("离开", style);
        leave.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                finish("离开安全屋。");
            }
        });
        content.add(leave).width(160).height(UiLayout.BUTTON_HEIGHT).padTop(UiLayout.SECTION_GAP).row();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
        }
        Gdx.gl.glClearColor(0.10f, 0.11f, 0.12f, 1f);
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
    @Override public void dispose() { stage.dispose(); }

    private void toggleEscapeMenu() {
        if (escapeMenu != null && escapeMenu.hasParent()) {
            escapeMenu.remove();
            escapeMenu = null;
            return;
        }
        escapeMenu = EscapeMenu.show(stage, game, () -> escapeMenu = null);
    }
}
