package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.view.FabricBookGame;

/**
 * Shared top status bar used by map-adjacent screens.
 */
public class TopStatusBar {

    public static final float HEIGHT = 52f;
    public static final float BUTTON_W = 150f;
    public static final float BUTTON_H = 42f;

    private final Player player;
    private final BitmapFont font;

    public TopStatusBar(Player player, BitmapFont font) {
        this.player = player;
        this.font = font;
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer,
                     OrthographicCamera camera, String layerText,
                     boolean showInventoryButtons, Vector2 mouse) {
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.86f, 0.86f, 0.86f, 1f);
        shapeRenderer.rect(0, FabricBookGame.SCREEN_HEIGHT - HEIGHT,
                FabricBookGame.SCREEN_WIDTH, HEIGHT);
        if (showInventoryButtons) {
            drawButtonBackground(shapeRenderer, FabricBookGame.SCREEN_WIDTH - 330, mouse);
            drawButtonBackground(shapeRenderer, FabricBookGame.SCREEN_WIDTH - 170, mouse);
        }
        shapeRenderer.end();

        batch.begin();

        Color old = font.getColor().cpy();
        font.setColor(1f, 0.38f, 0.42f, 1f);
        font.draw(batch, "生命 " + player.getHp() + "/" + player.getMaxHp(),
                10, FabricBookGame.SCREEN_HEIGHT - 10);

        font.setColor(0.95f, 0.82f, 0f, 1f);
        font.draw(batch, "金币 " + player.getGold(),
                210, FabricBookGame.SCREEN_HEIGHT - 10);

        font.setColor(Color.BLACK);
        font.draw(batch, "药水 " + player.getPotions().size() + "/3",
                360, FabricBookGame.SCREEN_HEIGHT - 10);
        for (int i = 0; i < player.getPotions().size(); i++) {
            font.draw(batch, player.getPotions().get(i).getName(),
                    455 + i * 88, FabricBookGame.SCREEN_HEIGHT - 10);
        }

        if (showInventoryButtons) {
            font.setColor(Color.WHITE);
            font.draw(batch, "卡牌", FabricBookGame.SCREEN_WIDTH - 282,
                    FabricBookGame.SCREEN_HEIGHT - 16);
            font.draw(batch, "藏品", FabricBookGame.SCREEN_WIDTH - 122,
                    FabricBookGame.SCREEN_HEIGHT - 16);
        }

        if (layerText != null && !layerText.isBlank()) {
            font.setColor(Color.BLACK);
            font.draw(batch, layerText, 740, FabricBookGame.SCREEN_HEIGHT - 10);
        }
        font.setColor(old);

        batch.end();
    }

    private void drawButtonBackground(ShapeRenderer shapeRenderer, float x, Vector2 mouse) {
        float y = FabricBookGame.SCREEN_HEIGHT - BUTTON_H - 5;
        boolean hover = mouse != null
                && mouse.x >= x && mouse.x <= x + BUTTON_W
                && mouse.y >= y && mouse.y <= y + BUTTON_H;
        if (hover) {
            shapeRenderer.setColor(0.46f, 0.46f, 0.46f, 1f);
        } else {
            shapeRenderer.setColor(0.38f, 0.38f, 0.38f, 1f);
        }
        shapeRenderer.rect(x, y, BUTTON_W, BUTTON_H);
    }
}
