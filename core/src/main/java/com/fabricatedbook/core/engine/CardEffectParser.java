package com.fabricatedbook.core.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static boolean isKnownType(String type) {
        return CardEffectType.fromType(type).isPresent();
    }

    public static Set<String> knownTypes() {
        return CardEffectType.knownIds();
    }

    public static boolean isExecutionSupported(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectType::supportsExecution)
                .orElse(false);
    }

    public static boolean hasExecutionHandler(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectExecutor::hasRegisteredHandler)
                .orElse(false);
    }

    public static Set<String> missingExecutionHandlers() {
        return Arrays.stream(CardEffectType.values())
                .filter(CardEffectType::supportsExecution)
                .filter(type -> !CardEffectExecutor.hasRegisteredHandler(type))
                .map(CardEffectType::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isPreviewSupported(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectType::supportsPreview)
                .orElse(false);
    }

    public static boolean hasPreviewHandler(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectPreviewer::hasRegisteredHandler)
                .orElse(false);
    }

    public static Set<String> missingPreviewHandlers() {
        return Arrays.stream(CardEffectType.values())
                .filter(CardEffectType::supportsPreview)
                .filter(type -> !CardEffectPreviewer.hasRegisteredHandler(type))
                .map(CardEffectType::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean hasValidArity(CardEffect effect) {
        if (effect == null) {
            return false;
        }
        return CardEffectType.fromType(effect.getType())
                .map(type -> type.acceptsPartCount(effect.parts().length))
                .orElse(false);
    }

    public static String expectedArity(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectType::expectedArity)
                .orElse("unknown");
    }

    public static boolean hasValidArgumentTypes(CardEffect effect) {
        if (effect == null) {
            return false;
        }
        return CardEffectType.fromType(effect.getType())
                .map(type -> type.acceptsArgumentTypes(effect.parts()))
                .orElse(false);
    }

    public static String expectedNumericParts(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectType::expectedNumericParts)
                .orElse("unknown");
    }

    public static boolean hasValidLiteralValues(CardEffect effect) {
        if (effect == null) {
            return false;
        }
        return CardEffectType.fromType(effect.getType())
                .map(type -> type.acceptsLiteralValues(effect.parts()))
                .orElse(false);
    }

    public static String expectedLiteralValues(String type) {
        return CardEffectType.fromType(type)
                .map(CardEffectType::expectedLiteralValues)
                .orElse("unknown");
    }
}
