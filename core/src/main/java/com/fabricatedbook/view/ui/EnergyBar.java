package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.fabricatedbook.core.entity.Player;

/**
 * EnergyBar — 能量条 UI
 * <p>
 * 在战斗界面显示玩家的当前能量值。
 * <p>
 * 引用方：BattleScreen（显示能量状态）
 */
public class EnergyBar extends Group {

    private final Player player;
    private final Label energyLabel;

    /**
     * 构造能量条。
     *
     * @param player 玩家实体
     */
    public EnergyBar(Player player) {
        this(player, new BitmapFont());
    }

    public EnergyBar(Player player, BitmapFont font) {
        this.player = player;

        energyLabel = new Label("", new Label.LabelStyle(
                font, Color.YELLOW));
        energyLabel.setFontScale(0.9f);
        addActor(energyLabel);

        update();
    }

    /**
     * 更新能量显示。
     */
    public void update() {
        energyLabel.setText("能量 " + player.getEnergy()
                + "/" + player.getMaxEnergy());
    }
}
