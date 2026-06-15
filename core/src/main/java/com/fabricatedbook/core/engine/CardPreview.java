package com.fabricatedbook.core.engine;

/**
 * Immutable preview data for a card while it is being aimed.
 */
public class CardPreview {

    private final String description;
    private final boolean hasPreview;

    public CardPreview(String description, boolean hasPreview) {
        this.description = description;
        this.hasPreview = hasPreview;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasPreview() {
        return hasPreview;
    }
}
