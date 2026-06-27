package com.fabricatedbook.core.engine;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of card effect DSL types and the backend surfaces that currently support them.
 */
public enum CardEffectType {
    DAMAGE("damage", true, true, 2, 3),
    DAMAGE_X("damage_x", true, true, 2, 2),
    DAMAGE_ALL("damage_all", true, true, 2, 3),
    DAMAGE_ALL_ATTACKING_INTENT("damage_all_attacking_intent", true, true, 2, 2),
    BLOCK("block", true, true, 2, 2),
    HEAL("heal", true, false, 2, 2),
    DRAW("draw", true, false, 2, 2),
    ENERGY("energy", true, false, 2, 2),
    DEBUFF("debuff", true, true, 3, 3),
    DEBUFF_ALL("debuff_all", true, true, 3, 3),
    BUFF("buff", true, true, 3, 5),
    PURIFY("purify", true, false, 1, 1),
    COUNTER("counter", true, true, 2, 2),
    BONUS_PER_ATTACK("bonus_per_attack", true, true, 2, 2),
    BONUS_LOW_HP("bonus_low_hp", true, true, 3, 3),
    DETONATE_WITHERING("detonate_withering", true, false, 2, 2),
    DOUBLE_POISON("double_poison", true, false, 1, 2),
    BLOCK_PER_TARGET("block_per_target", true, true, 2, 2),
    BONUS_PER_DAMAGE_TAKEN("bonus_per_damage_taken", true, true, 3, 3),
    ADD_RANDOM_ATTACK("add_random_attack", true, false, 1, 1),
    ADD_CARD_TO_DISCARD("add_card_to_discard", true, false, 2, 2),
    STUN_CHANCE("stun_chance", true, false, 2, 2),
    ESCALATING("escalating", true, true, 2, 2),
    CHANCE_DEBUFF("chance_debuff", true, false, 4, 4),
    POISON_CHANCE("poison_chance", true, false, 2, 3),
    TRIGGER_WITHERING("trigger_withering", true, false, 1, 2),
    END_TURN_DAMAGE("end_turn_damage", true, false, 2, 2);

    private static final Map<String, CardEffectType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CardEffectType::id, type -> type));
    private static final Set<String> KNOWN_IDS = Set.copyOf(BY_ID.keySet());

    private final String id;
    private final boolean executionSupported;
    private final boolean previewSupported;
    private final int minParts;
    private final int maxParts;

    CardEffectType(String id, boolean executionSupported, boolean previewSupported,
                   int minParts, int maxParts) {
        this.id = id;
        this.executionSupported = executionSupported;
        this.previewSupported = previewSupported;
        this.minParts = minParts;
        this.maxParts = maxParts;
    }

    public String id() {
        return id;
    }

    public boolean supportsExecution() {
        return executionSupported;
    }

    public boolean supportsPreview() {
        return previewSupported;
    }

    public boolean acceptsPartCount(int partCount) {
        return partCount >= minParts && (maxParts < 0 || partCount <= maxParts);
    }

    public String expectedArity() {
        if (minParts == maxParts) {
            return String.valueOf(minParts);
        }
        if (maxParts < 0) {
            return minParts + "+";
        }
        return minParts + "-" + maxParts;
    }

    public static Optional<CardEffectType> fromType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(type.toLowerCase()));
    }

    public static Set<String> knownIds() {
        return KNOWN_IDS;
    }
}
