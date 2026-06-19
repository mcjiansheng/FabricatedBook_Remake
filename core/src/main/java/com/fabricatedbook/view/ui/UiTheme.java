package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Shared visual tokens for the Scene2D UI and manual game renderers.
 *
 * <p>Callers must treat these colors as read-only. Components that need to
 * animate alpha or tint should work on {@link Color#cpy()} instead.</p>
 */
public final class UiTheme {

    public static final Color BATTLE_BACKGROUND = new Color(0.10f, 0.10f, 0.12f, 1f);
    public static final Color BATTLE_SURFACE = new Color(0.13f, 0.13f, 0.17f, 1f);
    public static final Color BATTLE_HAND = new Color(0.08f, 0.08f, 0.11f, 1f);
    public static final Color MAP_BACKGROUND = new Color(0.78f, 0.78f, 0.78f, 1f);
    public static final Color MAP_TOP_BAR = new Color(0.86f, 0.86f, 0.86f, 1f);
    public static final Color ACCENT_GOLD = new Color(0.95f, 0.82f, 0f, 1f);
    public static final Color HEALTH_TEXT = new Color(1f, 0.38f, 0.42f, 1f);

    public static final Color BUTTON_UP = new Color(0.36f, 0.36f, 0.36f, 1f);
    public static final Color BUTTON_OVER = new Color(0.46f, 0.46f, 0.46f, 1f);
    public static final Color BUTTON_DOWN = new Color(0.24f, 0.24f, 0.24f, 1f);
    public static final Color BUTTON_BORDER = new Color(0.08f, 0.08f, 0.08f, 1f);

    private UiTheme() {}
}
