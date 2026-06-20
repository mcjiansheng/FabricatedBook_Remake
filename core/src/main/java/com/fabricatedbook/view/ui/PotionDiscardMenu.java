package com.fabricatedbook.view.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.view.FabricBookGame;

/** Shared non-combat potion discard picker. */
public final class PotionDiscardMenu {
    private PotionDiscardMenu() {}

    public static Group show(Stage stage, FabricBookGame game, Player player, Runnable onClose) {
        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(modal);
        Table backdrop = new Table();
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        backdrop.setBackground(UiStyles.modalBackdrop());
        modal.addActor(backdrop);
        Table panel = new Table();
        panel.setBackground(UiStyles.panelSurface());
        panel.setSize(480, 360);
        panel.setPosition(400, 180);
        panel.pad(24);
        modal.addActor(panel);
        panel.add(new Label("丢弃药水", new Label.LabelStyle(game.getFontForScale(1.5f), UiTheme.ACCENT_GOLD))).padBottom(16);
        panel.row();
        if (player.getPotions().isEmpty()) {
            panel.add(new Label("当前没有药水。", new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.WHITE)));
            panel.row();
        }
        for (int i = 0; i < player.getPotions().size(); i++) {
            final int index = i;
            Potion potion = player.getPotions().get(i);
            TextButton button = new TextButton("丢弃 " + potion.getName(), UiStyles.buttonStyle(game));
            button.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    player.removePotion(index);
                    game.autosaveCurrentRun();
                    modal.remove();
                    if (onClose != null) onClose.run();
                }
            });
            panel.add(button).width(320).height(42).padBottom(8);
            panel.row();
        }
        TextButton close = new TextButton("关闭", UiStyles.buttonStyle(game));
        close.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                modal.remove();
                if (onClose != null) onClose.run();
            }
        });
        panel.add(close).width(180).height(42).padTop(8);
        modal.toFront();
        return modal;
    }
}
