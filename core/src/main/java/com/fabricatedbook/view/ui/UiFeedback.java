package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.fabricatedbook.view.FabricBookGame;

/** Short, color-coded feedback for a completed or rejected player action. */
public final class UiFeedback extends Label {
    public enum Tone { INFO, SUCCESS, WARNING, ERROR }

    public UiFeedback(FabricBookGame game) {
        super("", new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY));
    }

    public void show(String message, Tone tone) {
        setText(message == null ? "" : message);
        setColor(colorFor(tone));
        clearActions();
        getColor().a = 1f;
        addAction(Actions.sequence(Actions.delay(3.5f), Actions.fadeOut(0.45f)));
    }

    private Color colorFor(Tone tone) {
        return switch (tone == null ? Tone.INFO : tone) {
            case INFO -> Color.LIGHT_GRAY;
            case SUCCESS -> new Color(0.48f, 0.9f, 0.52f, 1f);
            case WARNING -> UiTheme.ACCENT_GOLD;
            case ERROR -> new Color(1f, 0.46f, 0.46f, 1f);
        };
    }
}
