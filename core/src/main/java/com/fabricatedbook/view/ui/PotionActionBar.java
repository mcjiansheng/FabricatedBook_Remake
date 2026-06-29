package com.fabricatedbook.view.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.view.FabricBookGame;

/** Upper-screen potion bar: click a potion, then choose an allowed action. */
public final class PotionActionBar extends Table {
    public interface PotionUseHandler { boolean use(Potion potion); }
    private final Stage stage;
    private final FabricBookGame game;
    private final Player player;
    private final boolean canUse;
    private final PotionUseHandler useHandler;
    private final Runnable onChanged;
    private Table actionMenu;

    public PotionActionBar(Stage stage, FabricBookGame game, Player player, boolean canUse,
                           PotionUseHandler useHandler, Runnable onChanged) {
        this.stage = stage; this.game = game; this.player = player; this.canUse = canUse;
        this.useHandler = useHandler; this.onChanged = onChanged;
        left(); rebuild();
    }
    public void rebuild() {
        clear();
        for (int i = 0; i < player.getPotions().size(); i++) {
            final int index = i;
            Potion potion = player.getPotions().get(i);
            TextButton button = new TextButton(potion.getName(), UiStyles.buttonStyle(game));
            UiTooltip.bind(button, stage, game, potion::getDescription);
            button.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                    showActions(index, button);
                }
            });
            add(button).width(118).height(34).padRight(6);
        }
    }

    private void showActions(int index, TextButton anchor) {
        if (index < 0 || index >= player.getPotions().size()) return;
        closeActions();
        actionMenu = new Table();
        actionMenu.setBackground(UiStyles.panelSurface());
        actionMenu.pad(6);
        if (canUse) {
            TextButton use = new TextButton("使用", UiStyles.buttonStyle(game));
            use.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                    Potion potion = player.removePotion(index);
                    if (potion != null && useHandler != null) useHandler.use(potion);
                    closeActions();
                    changed();
                }
            });
            actionMenu.add(use).width(116).height(30).padBottom(4);
            actionMenu.row();
        }
        TextButton discard = new TextButton("丢弃", UiStyles.buttonStyle(game));
        discard.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                player.removePotion(index);
                autosavePotionChange();
                closeActions();
                changed();
            }
        });
        actionMenu.add(discard).width(116).height(30).padBottom(4);
        actionMenu.row();
        TextButton cancel = new TextButton("取消", UiStyles.buttonStyle(game));
        cancel.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                closeActions();
            }
        });
        actionMenu.add(cancel).width(116).height(26);
        actionMenu.pack();

        Vector2 position = anchor.localToStageCoordinates(new Vector2(0, 0));
        float x = MathUtils.clamp(position.x, 4, stage.getWidth() - actionMenu.getWidth() - 4);
        float y = position.y - actionMenu.getHeight() - 6;
        if (y < 4) y = position.y + anchor.getHeight() + 6;
        actionMenu.setPosition(x, y);
        stage.addActor(actionMenu);
        actionMenu.toFront();
    }

    private void closeActions() {
        if (actionMenu != null) actionMenu.remove();
        actionMenu = null;
    }

    private void autosavePotionChange() {
        if (game.getCurrentRun() != null) {
            game.getCurrentRun().markActiveNodeProgressCommitted();
        }
        game.autosaveCurrentRun();
    }

    private void changed() { rebuild(); if (onChanged != null) onChanged.run(); }
}
