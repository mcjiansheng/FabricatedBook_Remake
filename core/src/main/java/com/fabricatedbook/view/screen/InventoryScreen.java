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
import com.fabricatedbook.view.ui.GameHud;

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
    private Label detailTitle;
    private Label detailBody;
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
        root.top().pad(92, 90, 36, 90);
        stage.addActor(root);

        new GameHud(stage, game, player,
                () -> returnMap != null ? returnMap.currentLayerStatusText() : "第" + player.getCurrentFloor() + "层",
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, Tab.CARDS)),
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, Tab.RELICS)),
                false, null, null);

        String titleText = selectedTab == Tab.CARDS ? "卡牌" : "藏品";
        Label title = new Label(titleText, new Label.LabelStyle(
                game.getFontForScale(1.8f), UiTheme.ACCENT_GOLD));
        root.add(title).padBottom(22);
        root.row();
        Table entries = new Table();
        entries.top().left().pad(14);
        entries.setBackground(UiStyles.panelSurface());
        if (selectedTab == Tab.CARDS) buildCardGrid(entries);
        else buildRelicGrid(entries);
        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        Table detail = new Table();
        detail.top().left().pad(18);
        detail.setBackground(UiStyles.panelSurface());
        detailTitle = new Label("选择一项", new Label.LabelStyle(game.getFontForScale(1.2f), UiTheme.ACCENT_GOLD));
        detailBody = new Label("点击卡牌或藏品查看完整说明。", new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY));
        detailBody.setWrap(true);
        detail.add(detailTitle).width(250).left().padBottom(14).row();
        detail.add(detailBody).width(250).top().left();
        Table body = new Table();
        body.add(scroll).width(650).height(500).top().left().padRight(18);
        body.add(detail).width(286).height(500).top().left();
        root.add(body);
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

    private void buildCardGrid(Table table) {
        List<Card> cards = allCards();
        if (cards.isEmpty()) {
            table.add(new Label("当前牌组为空。", new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY))).pad(16);
            return;
        }
        for (int index = 0; index < cards.size(); index++) {
            Card card = cards.get(index);
            TextButton tile = new TextButton("[" + card.getCost() + "]  " + card.getName()
                    + "\n" + card.getType() + " · " + card.getRarity(), UiStyles.buttonStyle(game));
            tile.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    showDetails(card.getName(), "费用：" + card.getCost() + "\n类型：" + card.getType()
                            + "\n稀有度：" + card.getRarity() + (card.isUpgraded() ? "\n已升级" : "")
                            + "\n\n" + card.getDescription());
                }
            });
            table.add(tile).width(195).height(104).pad(5);
            if ((index + 1) % 3 == 0) table.row();
        }
    }

    private void buildRelicGrid(Table table) {
        List<Relic> relics = player.getRelics();
        if (relics.isEmpty()) {
            table.add(new Label("尚未获得藏品\n在事件、奖励或商店中获得藏品。",
                    new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY))).pad(16);
            return;
        }
        for (int index = 0; index < relics.size(); index++) {
            Relic relic = relics.get(index);
            TextButton tile = new TextButton(relic.getName() + "\n" + relic.getRarity(), UiStyles.buttonStyle(game));
            tile.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    showDetails(relic.getName(), "稀有度：" + relic.getRarity() + "\n\n" + relic.getDescription());
                }
            });
            table.add(tile).width(195).height(92).pad(5);
            if ((index + 1) % 3 == 0) table.row();
        }
    }

    private void showDetails(String title, String body) {
        detailTitle.setText(title);
        detailBody.setText(body);
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
