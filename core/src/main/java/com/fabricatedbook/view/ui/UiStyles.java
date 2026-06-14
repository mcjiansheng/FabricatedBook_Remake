package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.fabricatedbook.view.FabricBookGame;

/** Shared lightweight UI styles for framed, stateful buttons. */
public final class UiStyles {

    private static NinePatchDrawable buttonUp;
    private static NinePatchDrawable buttonOver;
    private static NinePatchDrawable buttonDown;

    private UiStyles() {}

    public static TextButton.TextButtonStyle buttonStyle(FabricBookGame game) {
        return buttonStyle(game.getFont());
    }

    public static TextButton.TextButtonStyle buttonStyle(BitmapFont font) {
        ensureButtonDrawables();
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.overFontColor = Color.WHITE;
        style.downFontColor = Color.LIGHT_GRAY;
        style.up = buttonUp;
        style.over = buttonOver;
        style.down = buttonDown;
        return style;
    }

    private static void ensureButtonDrawables() {
        if (buttonUp != null) return;
        buttonUp = buttonDrawable(0.36f, 0.36f, 0.36f, 1f);
        buttonOver = buttonDrawable(0.46f, 0.46f, 0.46f, 1f);
        buttonDown = buttonDrawable(0.24f, 0.24f, 0.24f, 1f);
    }

    private static NinePatchDrawable buttonDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        pixmap.setColor(0.08f, 0.08f, 0.08f, 1f);
        pixmap.drawRectangle(0, 0, 8, 8);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        NinePatchDrawable drawable = new NinePatchDrawable(new NinePatch(texture,
                1, 1, 1, 1));
        drawable.setMinWidth(8);
        drawable.setMinHeight(8);
        return drawable;
    }
}
