package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.FabricBookGame;

import java.util.function.Supplier;

/**
 * The single persistent in-run header. Screens supply navigation and battle-only
 * potion behavior, while layout and live player state remain identical everywhere.
 */
public final class GameHud extends Group {
    public static final float HEIGHT = UiLayout.HUD_HEIGHT;

    private final Player player;
    private final Supplier<String> layerText;
    private final Label identityLabel;
    private final Label goldLabel;
    private final Label potionCapacityLabel;
    private final Label layerLabel;

    public GameHud(Stage stage, FabricBookGame game, Player player, Supplier<String> layerText,
                   Runnable showCards, Runnable showRelics, boolean canUsePotions,
                   PotionActionBar.PotionUseHandler potionUseHandler, Runnable onPotionChanged) {
        this.player = player;
        this.layerText = layerText;
        setBounds(0, FabricBookGame.SCREEN_HEIGHT - HEIGHT, FabricBookGame.SCREEN_WIDTH, HEIGHT);

        Image background = new Image(UiStyles.statusBarSurface());
        background.setBounds(0, 0, getWidth(), getHeight());
        addActor(background);

        identityLabel = label(game, Color.valueOf("E8545F"));
        identityLabel.setPosition(12, 20);
        addActor(identityLabel);
        goldLabel = label(game, UiTheme.ACCENT_GOLD);
        goldLabel.setPosition(215, 20);
        addActor(goldLabel);

        PotionActionBar potions = new PotionActionBar(stage, game, player, canUsePotions,
                potionUseHandler, onPotionChanged);
        potions.pack();
        float potionsX = (FabricBookGame.SCREEN_WIDTH - potions.getWidth()) / 2f;
        potions.setPosition(potionsX, 12);
        addActor(potions);
        potionCapacityLabel = label(game, Color.BLACK);
        potionCapacityLabel.setPosition(potionsX - 98, 20);
        addActor(potionCapacityLabel);

        layerLabel = label(game, Color.BLACK);
        layerLabel.setPosition(835, 20);
        addActor(layerLabel);
        TextButton cards = new TextButton("卡牌", UiStyles.buttonStyle(game));
        cards.setBounds(950, 8, 150, 42);
        cards.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (showCards != null) showCards.run();
            }
        });
        addActor(cards);
        TextButton relics = new TextButton("藏品", UiStyles.buttonStyle(game));
        relics.setBounds(1110, 8, 150, 42);
        relics.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (showRelics != null) showRelics.run();
            }
        });
        addActor(relics);
        stage.addActor(this);
        refresh();
    }

    private Label label(FabricBookGame game, Color color) {
        return new Label("", new Label.LabelStyle(game.getFont(), color));
    }

    @Override public void act(float delta) {
        super.act(delta);
        refresh();
    }

    private void refresh() {
        identityLabel.setText(player.getProfession().getDisplayName() + "  生命 "
                + player.getHp() + "/" + player.getMaxHp());
        goldLabel.setText("金币 " + player.getGold());
        potionCapacityLabel.setText("药水 " + player.getPotions().size() + "/" + player.getMaxPotionSlots());
        String value = layerText == null ? "" : layerText.get();
        layerLabel.setText(value == null ? "" : value);
    }
}
