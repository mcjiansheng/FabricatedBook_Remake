package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.Player;

/**
 * BuffBar — Buff 状态栏
 * <p>
 * 显示玩家身上的所有 Buff 名称和层数。
 * <p>
 * 引用方：BattleScreen（显示 Buff 状态）
 */
public class BuffBar extends Group {

    private final Player player;
    private final Label buffLabel;

    /**
     * 构造 Buff 状态栏。
     *
     * @param player 玩家实体
     */
    public BuffBar(Player player) {
        this.player = player;

        buffLabel = new Label("", new Label.LabelStyle(
                new com.badlogic.gdx.graphics.g2d.BitmapFont(),
                Color.CYAN));
        buffLabel.setFontScale(0.9f);
        addActor(buffLabel);

        update();
    }

    /**
     * 更新 Buff 显示。
     */
    public void update() {
        StringBuilder sb = new StringBuilder();
        for (BuffHook buff : player.getBuffs()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(buff.getBuffName()).append("[").append(buff.getStack()).append("]");
        }
        buffLabel.setText(sb.toString());
    }
}
