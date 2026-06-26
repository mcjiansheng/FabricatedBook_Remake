package com.fabricatedbook.core.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for the card effect DSL used by execution and preview code.
 */
public final class CardEffectParser {
    private CardEffectParser() {}

    public static List<CardEffect> parse(List<String> effects) {
        List<CardEffect> parsed = new ArrayList<>();
        if (effects == null) {
            return parsed;
        }
        for (String effect : effects) {
            CardEffect cardEffect = parse(effect);
            if (cardEffect != null) {
                parsed.add(cardEffect);
            }
        }
        return parsed;
    }

    public static CardEffect parse(String effect) {
        if (effect == null || effect.isBlank()) {
            return null;
        }
        String trimmed = effect.trim();
        return new CardEffect(trimmed, trimmed.split(":"));
    }
}
