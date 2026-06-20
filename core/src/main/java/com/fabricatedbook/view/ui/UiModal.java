package com.fabricatedbook.view.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.fabricatedbook.view.FabricBookGame;

/** Shared modal shell: a blocking backdrop plus a foreground panel owned by the caller. */
public final class UiModal {
    private UiModal() {}

    public static Group open(Stage stage) {
        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);

        Table backdrop = new Table();
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        backdrop.setBackground(UiStyles.modalBackdrop());
        modal.addActor(backdrop);

        stage.addActor(modal);
        modal.toFront();
        return modal;
    }

    public static Table panel(float width, float height) {
        Table panel = new Table();
        panel.setBackground(UiStyles.panelSurface());
        panel.setSize(width, height);
        panel.setPosition((FabricBookGame.SCREEN_WIDTH - width) / 2f,
                (FabricBookGame.SCREEN_HEIGHT - height) / 2f);
        return panel;
    }

    public static void close(Group modal) {
        if (modal != null) modal.remove();
    }
}
