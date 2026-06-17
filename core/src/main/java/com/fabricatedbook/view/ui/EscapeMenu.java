package com.fabricatedbook.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.screen.TitleScreen;

/**
 * Shared in-run Escape menu.
 */
public final class EscapeMenu {

    private EscapeMenu() {}

    public static Group show(Stage stage, FabricBookGame game, Runnable onClose) {
        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(modal);

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        modal.addActor(table);

        Label title = new Label("暂停", new Label.LabelStyle(
                game.getFontForScale(1.9f), Color.GOLD));
        table.add(title).padBottom(28);
        table.row();

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        TextButton resume = new TextButton("继续", style);
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                modal.remove();
                if (onClose != null) onClose.run();
            }
        });
        table.add(resume).width(240).height(54).padBottom(16);
        table.row();

        TextButton exit = new TextButton("保存并退出", style);
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.autosaveCurrentRun();
                Gdx.app.exit();
            }
        });
        table.add(exit).width(240).height(54).padBottom(16);
        table.row();

        TextButton abandon = new TextButton("放弃对局", style);
        abandon.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.abandonCurrentRun();
                game.setScreen(new TitleScreen(game));
            }
        });
        table.add(abandon).width(240).height(54);
        modal.toFront();
        return modal;
    }
}
