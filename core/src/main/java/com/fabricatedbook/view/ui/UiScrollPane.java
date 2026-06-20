package com.fabricatedbook.view.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

/** Common vertical scroll behavior for in-run lists and grids. */
public final class UiScrollPane {
    private UiScrollPane() {}

    public static ScrollPane vertical(Actor content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        pane.setOverscroll(false, false);
        return pane;
    }
}
