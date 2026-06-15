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
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;

import java.util.ArrayList;
import java.util.List;

/**
 * InventoryScreen — 地图上的卡牌/藏品查看界面。
 */
public class InventoryScreen implements Screen {

    private final FabricBookGame game;
    private final Player player;
    private final MapScreen returnMap;
    private Stage stage;

    public InventoryScreen(FabricBookGame game, Player player, MapScreen returnMap) {
        this.game = game;
        this.player = player;
        this.returnMap = returnMap;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(36);
        stage.addActor(root);

        Label title = new Label("卡牌与藏品", new Label.LabelStyle(
                game.getFontForScale(1.8f), Color.BLACK));
        root.add(title).colspan(2).padBottom(22);
        root.row();

        Table cards = new Table();
        cards.top().left();
        cards.add(new Label("卡牌", new Label.LabelStyle(game.getFont(), Color.BLACK))).left().padBottom(12);
        cards.row();
        for (Card card : allCards()) {
            Label label = new Label(card.getName() + "  用 " + card.getCost()
                    + "  " + card.getDescription(), new Label.LabelStyle(game.getFont(), Color.DARK_GRAY));
            label.setWrap(true);
            cards.add(label).width(520).left().padBottom(8);
            cards.row();
        }

        Table relics = new Table();
        relics.top().left();
        relics.add(new Label("藏品", new Label.LabelStyle(game.getFont(), Color.BLACK))).left().padBottom(12);
        relics.row();
        if (player.getRelics().isEmpty()) {
            relics.add(new Label("无", new Label.LabelStyle(game.getFont(), Color.DARK_GRAY))).left();
            relics.row();
        }
        for (Relic relic : player.getRelics()) {
            Label label = new Label(relic.getName() + "  "
                    + relic.getDescription(), new Label.LabelStyle(game.getFont(), Color.DARK_GRAY));
            label.setWrap(true);
            relics.add(label).width(520).left().padBottom(8);
            relics.row();
        }

        root.add(new ScrollPane(cards)).width(560).height(520).padRight(38);
        root.add(new ScrollPane(relics)).width(560).height(520);
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
        root.add(back).colspan(2).width(180).height(48).padTop(24);
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
        Gdx.gl.glClearColor(0.78f, 0.78f, 0.78f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() { stage.dispose(); }
}
