package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.ui.UiStyles;

import java.util.HashMap;
import java.util.Map;

/**
 * PlayerActor — 玩家 Actor
 * <p>
 * 在战斗界面中显示玩家立绘、生命值、格挡值、能量和 Buff 状态。
 * 根据职业加载对应的角色立绘。
 * <p>
 * 引用方：BattleScreen（创建和更新玩家显示）
 * 资源文件：img/Character_Selection_{职业}.png
 */
public class PlayerActor extends Actor {

    private final Player player;
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer;
    private Texture sprite;
    private boolean highlighted;
    private final Map<String, Texture> buffIcons = new HashMap<>();

    public static final float PLAYER_WIDTH = 150;
    public static final float PLAYER_HEIGHT = 180;

    /**
     * 构造玩家 Actor。
     *
     * @param player   玩家实体
     * @param font     字体
     * @param renderer ShapeRenderer
     */
    public PlayerActor(Player player, BitmapFont font, ShapeRenderer renderer) {
        this.player = player;
        this.font = font;
        this.shapeRenderer = renderer;
        setSize(PLAYER_WIDTH, PLAYER_HEIGHT);
        loadSprite();
        loadBuffIcons();
    }

    private void loadBuffIcons() {
        String[] names = {"Poison", "Fragile", "Weak", "Withering", "Strength",
                "Resistance", "ArmorBuff", "BlockIncrease", "BlockReduction",
                "Dizziness", "ExtraEnergyBuff", "UndeadBuff"};
        for (String name : names) {
            try {
                buffIcons.put(name, new Texture("img/" + buffIconFile(name) + ".png"));
            } catch (Exception ignored) {
                // Missing icons fall back to text in drawBuffs.
            }
        }
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    /** Compact tooltip text for the player's currently active buffs. */
    public String buffSummary() {
        StringBuilder summary = new StringBuilder();
        for (BuffHook buff : player.getBuffs()) {
            if (buff.getStack() <= 0) continue;
            if (summary.length() > 0) summary.append('\n');
            summary.append(buffLabel(buff.getBuffName())).append("：").append(buff.getStack());
        }
        return summary.toString();
    }

    /** 根据职业加载对应角色立绘 */
    private void loadSprite() {
        String fileName = switch (player.getProfession()) {
            case WARRIOR -> "Character_Selection_Warrior";
            case MAGE -> "Character_Selection_Mage";
            case WITCH -> "Character_Selection_Witch";
        };
        try {
            sprite = new Texture("img/" + fileName + ".png");
        } catch (Exception e) {
            sprite = null;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (highlighted) {
            rect(batch, new Color(0.3f, 0.65f, 1f, 0.35f), getX() - 12, getY() - 12,
                    PLAYER_WIDTH + 24, PLAYER_HEIGHT + 24);
        }
        float hpRatio = (float) player.getHp() / player.getMaxHp();
        float hpBarWidth = PLAYER_WIDTH - 20;
        // 背景
        rect(batch, new Color(0.3f, 0.3f, 0.3f, 1f), getX() + 10, getY() + 5, hpBarWidth, 10);
        // 血量
        if (hpRatio > 0.5f) {
            rect(batch, new Color(0.2f, 0.8f, 0.2f, 1f), getX() + 10, getY() + 5, hpBarWidth * hpRatio, 10);
        } else if (hpRatio > 0.25f) {
            rect(batch, new Color(0.8f, 0.8f, 0.2f, 1f), getX() + 10, getY() + 5, hpBarWidth * hpRatio, 10);
        } else {
            rect(batch, new Color(0.8f, 0.2f, 0.2f, 1f), getX() + 10, getY() + 5, hpBarWidth * hpRatio, 10);
        }

        // 格挡条
        if (player.getBlock() > 0) {
            rect(batch, new Color(0.3f, 0.5f, 0.9f, 1f), getX() + 10, getY() + 18, Math.min(hpBarWidth, player.getBlock() * 5), 5);
        }

        // 绘制角色立绘
        if (sprite != null) {
            float sWidth = Math.min(PLAYER_WIDTH - 20, sprite.getWidth());
            float sHeight = Math.min(PLAYER_HEIGHT - 60, sprite.getHeight());
            float sx = getX() + (PLAYER_WIDTH - sWidth) / 2;
            float sy = getY() + 35;
            batch.draw(sprite, sx, sy, sWidth, sHeight);
        }

        font.draw(batch, player.getHp() + "/" + player.getMaxHp(),
                getX() + 42, getY() + 17);
        if (player.getBlock() > 0) {
            font.draw(batch, String.valueOf(player.getBlock()),
                    getX() + 42, getY() + 31);
        }
        drawBuffs(batch);
    }

    private void rect(Batch batch, Color color, float x, float y, float width, float height) {
        Color old = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(UiStyles.pixelTexture(), x, y, width, height);
        batch.setColor(old);
    }

    private void drawBuffs(Batch batch) {
        float x = getX() + 8;
        float y = getY() - 30;
        int shown = 0;
        for (BuffHook buff : player.getBuffs()) {
            if (buff.getStack() <= 0) continue;
            Texture icon = buffIcons.get(buff.getBuffName());
            float iconX = x + shown * 38f;
            if (icon != null) {
                batch.draw(icon, iconX, y, 26, 26);
            } else {
                font.draw(batch, buffLabel(buff.getBuffName()), iconX, y + 22);
            }
            font.draw(batch, String.valueOf(buff.getStack()), iconX + 20, y + 10);
            shown++;
        }
    }

    private String buffIconFile(String buffName) {
        return switch (buffName) {
            case "Poison" -> "poisoning";
            case "Fragile" -> "fragile";
            case "Weak" -> "weak";
            case "Withering" -> "withering";
            case "Strength" -> "strength";
            case "Resistance" -> "resistance";
            case "ArmorBuff" -> "armor";
            case "BlockIncrease" -> "block_increase";
            case "BlockReduction" -> "block_reduction";
            case "Dizziness" -> "dizziness";
            case "ExtraEnergyBuff" -> "extra_energy";
            case "UndeadBuff" -> "undead";
            default -> "ukn";
        };
    }

    private String buffLabel(String buffName) {
        return switch (buffName) {
            case "Poison" -> "毒";
            case "Fragile" -> "脆";
            case "Weak" -> "弱";
            case "Withering" -> "凋";
            case "Strength" -> "力";
            case "ArmorBuff" -> "甲";
            case "ExtraEnergyBuff" -> "能";
            case "UndeadBuff" -> "死";
            default -> "?";
        };
    }

    /**
     * 释放纹理资源。
     */
    public void dispose() {
        if (sprite != null) sprite.dispose();
        for (Texture texture : buffIcons.values()) {
            texture.dispose();
        }
    }
}
