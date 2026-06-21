package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiTheme;

/**
 * EndingScreen — 结局界面。
 */
public class EndingScreen implements Screen {

    public enum EndingType {
        INTERRUPTED,
        NORMAL,
        HIDDEN
    }

    private final FabricBookGame game;
    private final EndingType endingType;
    private Stage stage;

    public EndingScreen(FabricBookGame game, EndingType endingType) {
        this.game = game;
        this.endingType = endingType == null ? EndingType.NORMAL : endingType;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(UiStyles.panelSurface());
        panel.pad(40);
        root.add(panel).width(900).height(500);

        Label title = new Label(titleText(), new Label.LabelStyle(
                game.getFontForScale(2.0f), Color.GOLD));
        title.setAlignment(Align.center);
        panel.add(title).width(800).padBottom(48);
        panel.row();

        Label body = new Label(bodyText(), new Label.LabelStyle(
                game.getFontForScale(1.05f), Color.WHITE));
        body.setWrap(true);
        body.setAlignment(Align.center);
        panel.add(body).width(800).padBottom(54);
        panel.row();

        TextButton back = new TextButton("返回标题", UiStyles.buttonStyle(game));
        back.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.abandonCurrentRun();
                game.setScreen(new TitleScreen(game));
            }
        });
        panel.add(back).width(220).height(56);
    }

    private String titleText() {
        return switch (endingType) {
            case INTERRUPTED -> "讲述中断";
            case HIDDEN -> "沉入虚妄";
            case NORMAL -> "故事终局";
        };
    }

    private String bodyText() {
        return switch (endingType) {
            case INTERRUPTED -> "这一切都太过诡异，你决定立马离开。书页在身后合上，故事没有继续讲下去。";
            case HIDDEN -> "诸天神魔？以玩弄人间为乐。你跨过高塔与傀儡的幕布，看见了故事背后的手。反抗并不轻松，但至少这一次，凡人没有低头。";
            case NORMAL -> "你战胜了一个俗套故事中的最终 Boss。魔王倒下，高塔归于沉寂，故事抵达它被安排好的终点。";
        };
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(UiTheme.TITLE_BACKGROUND.r, UiTheme.TITLE_BACKGROUND.g,
                UiTheme.TITLE_BACKGROUND.b, 1);
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
