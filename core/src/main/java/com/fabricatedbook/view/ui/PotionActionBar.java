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

/** Upper-screen potion bar: click a potion, then choose an allowed action. */
public final class PotionActionBar extends Table {
    public interface PotionUseHandler { boolean use(Potion potion); }
    private final Stage stage;
    private final FabricBookGame game;
    private final Player player;
    private final boolean canUse;
    private final PotionUseHandler useHandler;
    private final Runnable onChanged;

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
            TextButton button = new TextButton(player.getPotions().get(i).getName(), UiStyles.buttonStyle(game));
            button.addListener(new ClickListener() { @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) { showActions(index); }});
            add(button).width(118).height(34).padRight(6);
        }
    }
    private void showActions(int index) {
        if (index < 0 || index >= player.getPotions().size()) return;
        Potion potion = player.getPotions().get(index);
        Group modal = new Group(); modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT); stage.addActor(modal);
        Table backdrop = new Table(); backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT); backdrop.setBackground(UiStyles.modalBackdrop()); modal.addActor(backdrop);
        Table panel = new Table(); panel.setBackground(UiStyles.panelSurface()); panel.setSize(360, canUse ? 220 : 165); panel.setPosition(460, 280); panel.pad(20); modal.addActor(panel);
        panel.add(new Label(potion.getName(), new Label.LabelStyle(game.getFontForScale(1.3f), UiTheme.ACCENT_GOLD))).padBottom(14); panel.row();
        if (canUse) { TextButton use = new TextButton("使用", UiStyles.buttonStyle(game)); use.addListener(new ClickListener(){@Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e,float x,float y){ Potion p=player.removePotion(index); if(p!=null && useHandler!=null) useHandler.use(p); modal.remove(); changed(); }}); panel.add(use).width(220).height(40).padBottom(8); panel.row(); }
        TextButton discard = new TextButton("丢弃", UiStyles.buttonStyle(game)); discard.addListener(new ClickListener(){@Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e,float x,float y){ player.removePotion(index); game.autosaveCurrentRun(); modal.remove(); changed(); }}); panel.add(discard).width(220).height(40).padBottom(8); panel.row();
        TextButton cancel = new TextButton("取消", UiStyles.buttonStyle(game)); cancel.addListener(new ClickListener(){@Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e,float x,float y){modal.remove();}}); panel.add(cancel).width(180).height(36);
        modal.toFront();
    }
    private void changed(){ rebuild(); if(onChanged!=null) onChanged.run(); }
}
