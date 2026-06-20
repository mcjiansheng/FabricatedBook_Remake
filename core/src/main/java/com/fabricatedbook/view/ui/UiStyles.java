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
    private static NinePatchDrawable buttonDisabled;
    private static Texture buttonUpTexture;
    private static Texture buttonOverTexture;
    private static Texture buttonDownTexture;
    private static Texture buttonDisabledTexture;
    private static NinePatchDrawable panelSurface;
    private static NinePatchDrawable modalBackdrop;
    private static NinePatchDrawable statusBarSurface;
    private static Texture panelSurfaceTexture;
    private static Texture modalBackdropTexture;
    private static Texture statusBarSurfaceTexture;

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
        style.disabled = buttonDisabled;
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

    /** Light, unobtrusive surface reserved for the always-visible game HUD. */
    public static NinePatchDrawable statusBarSurface() {
        ensureSurfaces();
        return statusBarSurface;
    }

    private static void ensureButtonDrawables() {
        if (buttonUp != null) return;
        buttonUpTexture = buttonTexture(UiTheme.BUTTON_UP);
        buttonOverTexture = buttonTexture(UiTheme.BUTTON_OVER);
        buttonDownTexture = buttonTexture(UiTheme.BUTTON_DOWN);
        buttonDisabledTexture = buttonTexture(new Color(0.25f, 0.25f, 0.25f, 1f));
        buttonUp = new NinePatchDrawable(new NinePatch(buttonUpTexture, 1, 1, 1, 1));
        buttonOver = new NinePatchDrawable(new NinePatch(buttonOverTexture, 1, 1, 1, 1));
        buttonDown = new NinePatchDrawable(new NinePatch(buttonDownTexture, 1, 1, 1, 1));
        buttonDisabled = new NinePatchDrawable(new NinePatch(buttonDisabledTexture, 1, 1, 1, 1));
        buttonUp.setMinWidth(8);
        buttonUp.setMinHeight(8);
        buttonOver.setMinWidth(8);
        buttonOver.setMinHeight(8);
        buttonDown.setMinWidth(8);
        buttonDown.setMinHeight(8);
        buttonDisabled.setMinWidth(8);
        buttonDisabled.setMinHeight(8);
    }

    private static void ensureSurfaces() {
        if (panelSurface != null) return;
        panelSurfaceTexture = solidTexture(UiTheme.BATTLE_SURFACE);
        modalBackdropTexture = solidTexture(new Color(0f, 0f, 0f, 0.70f));
        statusBarSurfaceTexture = solidTexture(new Color(0.86f, 0.86f, 0.86f, 1f));
        panelSurface = new NinePatchDrawable(new NinePatch(panelSurfaceTexture, 1, 1, 1, 1));
        modalBackdrop = new NinePatchDrawable(new NinePatch(modalBackdropTexture, 1, 1, 1, 1));
        statusBarSurface = new NinePatchDrawable(new NinePatch(statusBarSurfaceTexture, 1, 1, 1, 1));
    }

    /** Releases globally cached generated textures during application shutdown. */
    public static void dispose() {
        if (buttonUpTexture != null) buttonUpTexture.dispose();
        if (buttonOverTexture != null) buttonOverTexture.dispose();
        if (buttonDownTexture != null) buttonDownTexture.dispose();
        if (buttonDisabledTexture != null) buttonDisabledTexture.dispose();
        if (panelSurfaceTexture != null) panelSurfaceTexture.dispose();
        if (modalBackdropTexture != null) modalBackdropTexture.dispose();
        if (statusBarSurfaceTexture != null) statusBarSurfaceTexture.dispose();
        buttonUpTexture = null;
        buttonOverTexture = null;
        buttonDownTexture = null;
        buttonDisabledTexture = null;
        panelSurfaceTexture = null;
        modalBackdropTexture = null;
        statusBarSurfaceTexture = null;
        buttonUp = null;
        buttonOver = null;
        buttonDown = null;
        buttonDisabled = null;
        panelSurface = null;
        modalBackdrop = null;
        statusBarSurface = null;
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
