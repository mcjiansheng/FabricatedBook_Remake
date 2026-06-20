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
        Group modal = UiModal.open(stage);
        Table table = UiModal.panel(360, 330);
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
                UiModal.close(modal);
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
                showAbandonConfirmation(stage, game);
            }
        });
        table.add(abandon).width(240).height(54);
        return modal;
    }

    private static void showAbandonConfirmation(Stage stage, FabricBookGame game) {
        Group confirmation = UiModal.open(stage);
        Table panel = UiModal.panel(460, 270);
        panel.pad(UiLayout.PANEL_PADDING);
        confirmation.addActor(panel);

        Label title = new Label("放弃本次对局？", new Label.LabelStyle(
                game.getFontForScale(1.45f), Color.GOLD));
        panel.add(title).padBottom(18);
        panel.row();
        Label detail = new Label("当前进度将被清除，且无法恢复。",
                new Label.LabelStyle(game.getFont(), Color.WHITE));
        panel.add(detail).padBottom(24);
        panel.row();

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        TextButton cancel = new TextButton("取消", style);
        cancel.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                UiModal.close(confirmation);
            }
        });
        TextButton confirm = new TextButton("确认放弃", style);
        confirm.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                game.abandonCurrentRun();
                game.setScreen(new TitleScreen(game));
            }
        });
        panel.add(cancel).width(160).height(UiLayout.BUTTON_HEIGHT).padRight(12);
        panel.add(confirm).width(160).height(UiLayout.BUTTON_HEIGHT);
    }
}
