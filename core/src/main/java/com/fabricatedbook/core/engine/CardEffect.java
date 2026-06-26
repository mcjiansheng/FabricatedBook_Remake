package com.fabricatedbook.core.engine;

import java.util.Arrays;

/**
 * Parsed representation of a card effect DSL string.
 */
public class CardEffect {
    private final String raw;
    private final String type;
    private final String[] parts;

    CardEffect(String raw, String[] parts) {
        this.raw = raw;
        this.parts = parts;
        this.type = parts.length == 0 ? "" : parts[0].toLowerCase();
    }

    public String getRaw() { return raw; }
    public String getType() { return type; }
    public String[] parts() { return Arrays.copyOf(parts, parts.length); }
}
