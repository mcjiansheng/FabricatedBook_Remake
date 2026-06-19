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
    private static Texture buttonUpTexture;
    private static Texture buttonOverTexture;
    private static Texture buttonDownTexture;
    private static NinePatchDrawable panelSurface;
    private static NinePatchDrawable modalBackdrop;
    private static Texture panelSurfaceTexture;
    private static Texture modalBackdropTexture;

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

    public static NinePatchDrawable panelSurface() {
        ensureSurfaces();
        return panelSurface;
    }

    public static NinePatchDrawable modalBackdrop() {
        ensureSurfaces();
        return modalBackdrop;
    }

    private static void ensureButtonDrawables() {
        if (buttonUp != null) return;
        buttonUpTexture = buttonTexture(UiTheme.BUTTON_UP);
        buttonOverTexture = buttonTexture(UiTheme.BUTTON_OVER);
        buttonDownTexture = buttonTexture(UiTheme.BUTTON_DOWN);
        buttonUp = new NinePatchDrawable(new NinePatch(buttonUpTexture, 1, 1, 1, 1));
        buttonOver = new NinePatchDrawable(new NinePatch(buttonOverTexture, 1, 1, 1, 1));
        buttonDown = new NinePatchDrawable(new NinePatch(buttonDownTexture, 1, 1, 1, 1));
        buttonUp.setMinWidth(8);
        buttonUp.setMinHeight(8);
        buttonOver.setMinWidth(8);
        buttonOver.setMinHeight(8);
        buttonDown.setMinWidth(8);
        buttonDown.setMinHeight(8);
    }

    private static void ensureSurfaces() {
        if (panelSurface != null) return;
        panelSurfaceTexture = solidTexture(UiTheme.BATTLE_SURFACE);
        modalBackdropTexture = solidTexture(new Color(0f, 0f, 0f, 0.70f));
        panelSurface = new NinePatchDrawable(new NinePatch(panelSurfaceTexture, 1, 1, 1, 1));
        modalBackdrop = new NinePatchDrawable(new NinePatch(modalBackdropTexture, 1, 1, 1, 1));
    }

    /** Releases globally cached generated textures during application shutdown. */
    public static void dispose() {
        if (buttonUpTexture != null) buttonUpTexture.dispose();
        if (buttonOverTexture != null) buttonOverTexture.dispose();
        if (buttonDownTexture != null) buttonDownTexture.dispose();
        if (panelSurfaceTexture != null) panelSurfaceTexture.dispose();
        if (modalBackdropTexture != null) modalBackdropTexture.dispose();
        buttonUpTexture = null;
        buttonOverTexture = null;
        buttonDownTexture = null;
        panelSurfaceTexture = null;
        modalBackdropTexture = null;
        buttonUp = null;
        buttonOver = null;
        buttonDown = null;
        panelSurface = null;
        modalBackdrop = null;
    }

    private static Texture buttonTexture(Color color) {
        return solidTexture(color, true);
    }

    private static Texture solidTexture(Color color) {
        return solidTexture(color, false);
    }

    private static Texture solidTexture(Color color, boolean border) {
        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        if (border) {
            pixmap.setColor(UiTheme.BUTTON_BORDER);
            pixmap.drawRectangle(0, 0, 8, 8);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
