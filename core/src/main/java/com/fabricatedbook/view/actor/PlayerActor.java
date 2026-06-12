package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;

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
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
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
        batch.end();

        // 绘制生命条
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (highlighted) {
            shapeRenderer.setColor(0.3f, 0.65f, 1f, 0.35f);
            shapeRenderer.rect(getX() - 12, getY() - 12,
                    PLAYER_WIDTH + 24, PLAYER_HEIGHT + 24);
        }
        float hpRatio = (float) player.getHp() / player.getMaxHp();
        float hpBarWidth = PLAYER_WIDTH - 20;
        // 背景
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(getX() + 10, getY() + 5, hpBarWidth, 10);
        // 血量
        if (hpRatio > 0.5f) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
        } else if (hpRatio > 0.25f) {
            shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 1f);
        } else {
            shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f);
        }
        shapeRenderer.rect(getX() + 10, getY() + 5, hpBarWidth * hpRatio, 10);

        // 格挡条
        if (player.getBlock() > 0) {
            shapeRenderer.setColor(0.3f, 0.5f, 0.9f, 1f);
            shapeRenderer.rect(getX() + 10, getY() + 18, Math.min(hpBarWidth, player.getBlock() * 5), 5);
        }
        shapeRenderer.end();

        if (highlighted) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.65f, 0.90f, 1f, 1f);
            shapeRenderer.rect(getX() - 12, getY() - 12,
                    PLAYER_WIDTH + 24, PLAYER_HEIGHT + 24);
            shapeRenderer.end();
        }

        batch.begin();

        // 绘制角色立绘
        if (sprite != null) {
            float sWidth = Math.min(PLAYER_WIDTH - 20, sprite.getWidth());
            float sHeight = Math.min(PLAYER_HEIGHT - 60, sprite.getHeight());
            float sx = getX() + (PLAYER_WIDTH - sWidth) / 2;
            float sy = getY() + 35;
            batch.draw(sprite, sx, sy, sWidth, sHeight);
        }

        // 绘制职业名称
        String profName = switch (player.getProfession()) {
            case WARRIOR -> "战士";
            case MAGE -> "法师";
            case WITCH -> "女巫";
        };
        font.draw(batch, profName, getX() + 10, getY() + PLAYER_HEIGHT - 5);

        // 绘制生命值
        font.draw(batch, "生命 " + player.getHp() + "/" + player.getMaxHp(),
                getX() + 10, getY() + PLAYER_HEIGHT - 20);

        // 绘制格挡
        if (player.getBlock() > 0) {
            font.draw(batch, "格挡 " + player.getBlock(),
                    getX() + 10, getY() + PLAYER_HEIGHT - 35);
        }

        // 绘制能量
        font.draw(batch, "能量 " + player.getEnergy(),
                getX() + 10, getY() + PLAYER_HEIGHT - 50);

        // 绘制金币
        font.draw(batch, "金币 " + player.getGold(),
                getX() + 10, getY() + PLAYER_HEIGHT - 65);
    }

    /**
     * 释放纹理资源。
     */
    public void dispose() {
        if (sprite != null) sprite.dispose();
    }
}
