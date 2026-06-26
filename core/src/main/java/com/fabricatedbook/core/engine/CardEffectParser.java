package com.fabricatedbook.core.engine;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;

/**
 * Parser for the card effect DSL used by execution and preview code.
 */
public final class CardEffectParser {
    private static final Set<String> KNOWN_TYPES = Set.of(
            "damage",
            "damage_x",
            "damage_all",
            "damage_all_attacking_intent",
            "block",
            "heal",
            "draw",
            "energy",
            "debuff",
            "debuff_all",
            "buff",
            "purify",
            "counter",
            "bonus_per_attack",
            "bonus_low_hp",
            "detonate_withering",
            "double_poison",
            "block_per_target",
            "bonus_per_damage_taken",
            "add_random_attack",
            "add_card_to_discard",
            "stun_chance",
            "escalating",
            "chance_debuff",
            "poison_chance",
            "trigger_withering",
            "end_turn_damage"
    );

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

    public static boolean isKnownType(String type) {
        return type != null && KNOWN_TYPES.contains(type.toLowerCase());
    }

    public static Set<String> knownTypes() {
        return KNOWN_TYPES;
    }
}
