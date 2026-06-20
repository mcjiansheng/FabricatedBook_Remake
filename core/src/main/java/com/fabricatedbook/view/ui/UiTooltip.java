package com.fabricatedbook.view.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.fabricatedbook.view.FabricBookGame;

import java.util.function.Supplier;

/** Lightweight hover detail surface that avoids expanding the main layout. */
public final class UiTooltip {
    private UiTooltip() {}

    public static void bind(Actor anchor, Stage stage, FabricBookGame game,
                            Supplier<String> textSupplier) {
        anchor.addListener(new InputListener() {
            private Table tooltip;

            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                String text = textSupplier == null ? "" : textSupplier.get();
                if (text == null || text.isBlank()) return;
                close();
                tooltip = new Table();
                tooltip.setBackground(UiStyles.panelSurface());
                tooltip.pad(UiLayout.PANEL_PADDING / 2f);
                Label label = new Label(text, new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
                label.setWrap(true);
                tooltip.add(label).width(250).left();
                tooltip.pack();

                Vector2 above = anchor.localToStageCoordinates(new Vector2(0, anchor.getHeight()));
                float tooltipX = MathUtils.clamp(above.x, 4, stage.getWidth() - tooltip.getWidth() - 4);
                float tooltipY = above.y + 8;
                if (tooltipY + tooltip.getHeight() > stage.getHeight() - 4) {
                    Vector2 below = anchor.localToStageCoordinates(new Vector2(0, 0));
                    tooltipY = Math.max(4, below.y - tooltip.getHeight() - 8);
                }
                tooltip.setPosition(tooltipX, tooltipY);
                stage.addActor(tooltip);
                tooltip.toFront();
            }

            @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                close();
            }

            private void close() {
                if (tooltip != null) tooltip.remove();
                tooltip = null;
            }
        });
    }
}
