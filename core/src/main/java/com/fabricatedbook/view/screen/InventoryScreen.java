package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * InventoryScreen — 地图上的卡牌/藏品查看界面。
 */
public class InventoryScreen implements Screen {

    public enum Tab { CARDS, RELICS }

    private final FabricBookGame game;
    private final Player player;
    private final MapScreen returnMap;
    private final Tab selectedTab;
    private Stage stage;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;

    public InventoryScreen(FabricBookGame game, Player player, MapScreen returnMap) {
        this(game, player, returnMap, Tab.CARDS);
    }

    public InventoryScreen(FabricBookGame game, Player player, MapScreen returnMap, Tab selectedTab) {
        this.game = game;
        this.player = player;
        this.returnMap = returnMap;
        this.selectedTab = selectedTab == null ? Tab.CARDS : selectedTab;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(36, 90, 36, 90);
        stage.addActor(root);

        String titleText = selectedTab == Tab.CARDS ? "卡牌" : "藏品";
        Label title = new Label(titleText, new Label.LabelStyle(
                game.getFontForScale(1.8f), UiTheme.ACCENT_GOLD));
        root.add(title).padBottom(22);
        root.row();
        Table entries = new Table();
        entries.top().left().pad(20);
        entries.setBackground(UiStyles.panelSurface());
        if (selectedTab == Tab.CARDS) {
            for (Card card : allCards()) addEntry(entries, card.getName() + "  ·  用 "
                    + card.getCost(), card.getDescription());
        } else if (player.getRelics().isEmpty()) {
            addEntry(entries, "尚未获得藏品", "在事件、奖励或商店中获得藏品。 ");
        } else {
            for (Relic relic : player.getRelics()) addEntry(entries, relic.getName(), relic.getDescription());
        }
        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        root.add(scroll).width(900).height(520);
        root.row();

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        TextButton back = new TextButton("返回地图", style);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.setScreen(returnMap);
            }
        });
        root.add(back).width(180).height(48).padTop(24);
    }

    private void addEntry(Table table, String title, String description) {
        Label name = new Label(title, new Label.LabelStyle(game.getFont(), Color.WHITE));
        Label body = new Label(description, new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY));
        body.setWrap(true);
        table.add(name).width(240).left().pad(8);
        table.add(body).width(580).left().pad(8);
        table.row();
    }

    private List<Card> allCards() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(player.getDrawPile());
        cards.addAll(player.getHand());
        cards.addAll(player.getDiscardPile());
        cards.addAll(player.getExhaustPile());
        return cards;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
        }
        Gdx.gl.glClearColor(UiTheme.MAP_BACKGROUND.r, UiTheme.MAP_BACKGROUND.g, UiTheme.MAP_BACKGROUND.b, 1f);
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
