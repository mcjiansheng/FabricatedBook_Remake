package com.fabricatedbook.view.ui;

import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fabricatedbook.view.FabricBookGame;

/**
 * Creates viewports that keep the game UI in the 1280x720 design space while
 * scaling with the desktop window or fullscreen resolution.
 */
public final class ResponsiveViewport {

    private ResponsiveViewport() {}

    public static Viewport create() {
        return new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }
}
