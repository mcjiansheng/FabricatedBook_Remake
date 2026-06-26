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
    DAMAGE("damage", true, true),
    DAMAGE_X("damage_x", true, true),
    DAMAGE_ALL("damage_all", true, true),
    DAMAGE_ALL_ATTACKING_INTENT("damage_all_attacking_intent", true, true),
    BLOCK("block", true, true),
    HEAL("heal", true, false),
    DRAW("draw", true, false),
    ENERGY("energy", true, false),
    DEBUFF("debuff", true, true),
    DEBUFF_ALL("debuff_all", true, true),
    BUFF("buff", true, true),
    PURIFY("purify", true, false),
    COUNTER("counter", true, true),
    BONUS_PER_ATTACK("bonus_per_attack", true, true),
    BONUS_LOW_HP("bonus_low_hp", true, true),
    DETONATE_WITHERING("detonate_withering", true, false),
    DOUBLE_POISON("double_poison", true, false),
    BLOCK_PER_TARGET("block_per_target", true, true),
    BONUS_PER_DAMAGE_TAKEN("bonus_per_damage_taken", true, true),
    ADD_RANDOM_ATTACK("add_random_attack", true, false),
    ADD_CARD_TO_DISCARD("add_card_to_discard", true, false),
    STUN_CHANCE("stun_chance", true, false),
    ESCALATING("escalating", true, true),
    CHANCE_DEBUFF("chance_debuff", true, false),
    POISON_CHANCE("poison_chance", true, false),
    TRIGGER_WITHERING("trigger_withering", true, false),
    END_TURN_DAMAGE("end_turn_damage", true, false);

    private static final Map<String, CardEffectType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CardEffectType::id, type -> type));
    private static final Set<String> KNOWN_IDS = Set.copyOf(BY_ID.keySet());

    private final String id;
    private final boolean executionSupported;
    private final boolean previewSupported;

    CardEffectType(String id, boolean executionSupported, boolean previewSupported) {
        this.id = id;
        this.executionSupported = executionSupported;
        this.previewSupported = previewSupported;
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
