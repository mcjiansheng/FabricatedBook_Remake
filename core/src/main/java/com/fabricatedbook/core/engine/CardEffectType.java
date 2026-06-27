package com.fabricatedbook.core.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of card effect DSL types and the backend surfaces that currently support them.
 */
public enum CardEffectType {
    DAMAGE("damage", true, true, 2, 3, 1, 2),
    DAMAGE_X("damage_x", true, true, 2, 2, 1),
    DAMAGE_ALL("damage_all", true, true, 2, 3, 1, 2),
    DAMAGE_ALL_ATTACKING_INTENT("damage_all_attacking_intent", true, true, 2, 2, 1),
    BLOCK("block", true, true, 2, 2, 1),
    HEAL("heal", true, false, 2, 2, 1),
    DRAW("draw", true, false, 2, 2, 1),
    ENERGY("energy", true, false, 2, 2, 1),
    DEBUFF("debuff", true, true, 3, 3, 2),
    DEBUFF_ALL("debuff_all", true, true, 3, 3, 2),
    BUFF("buff", true, true, 3, 5, 3, 4),
    PURIFY("purify", true, false, 1, 1),
    COUNTER("counter", true, true, 2, 2),
    BONUS_PER_ATTACK("bonus_per_attack", true, true, 2, 2, 1),
    BONUS_LOW_HP("bonus_low_hp", true, true, 3, 3, 1, 2),
    DETONATE_WITHERING("detonate_withering", true, false, 2, 2, 1),
    DOUBLE_POISON("double_poison", true, false, 1, 2, 1),
    BLOCK_PER_TARGET("block_per_target", true, true, 2, 2, 1),
    BONUS_PER_DAMAGE_TAKEN("bonus_per_damage_taken", true, true, 3, 3, 1, 2),
    ADD_RANDOM_ATTACK("add_random_attack", true, false, 1, 1),
    ADD_CARD_TO_DISCARD("add_card_to_discard", true, false, 2, 2),
    STUN_CHANCE("stun_chance", true, false, 2, 2, 1),
    ESCALATING("escalating", true, true, 2, 2, 1),
    CHANCE_DEBUFF("chance_debuff", true, false, 4, 4, 2, 3),
    POISON_CHANCE("poison_chance", true, false, 2, 3, 1, 2),
    TRIGGER_WITHERING("trigger_withering", true, false, 1, 2, 1),
    END_TURN_DAMAGE("end_turn_damage", true, false, 2, 2, 1);

    private static final Map<String, CardEffectType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CardEffectType::id, type -> type));
    private static final Set<String> KNOWN_IDS = Set.copyOf(BY_ID.keySet());

    private final String id;
    private final boolean executionSupported;
    private final boolean previewSupported;
    private final int minParts;
    private final int maxParts;
    private final Set<Integer> numericPartIndexes;

    CardEffectType(String id, boolean executionSupported, boolean previewSupported,
                   int minParts, int maxParts, int... numericPartIndexes) {
        this.id = id;
        this.executionSupported = executionSupported;
        this.previewSupported = previewSupported;
        this.minParts = minParts;
        this.maxParts = maxParts;
        this.numericPartIndexes = new HashSet<>();
        for (int index : numericPartIndexes) {
            this.numericPartIndexes.add(index);
        }
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

    public boolean acceptsArgumentTypes(String[] parts) {
        if (parts == null) {
            return false;
        }
        for (int index : numericPartIndexes) {
            if (index >= parts.length) {
                continue;
            }
            if (!isInteger(parts[index])) {
                return false;
            }
        }
        return true;
    }

    public String expectedNumericParts() {
        if (numericPartIndexes.isEmpty()) {
            return "none";
        }
        return numericPartIndexes.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
