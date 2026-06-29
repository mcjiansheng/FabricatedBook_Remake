package com.fabricatedbook.core.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardEffectParserTest {
    @Test
    void parsesTypeCaseInsensitivelyAndKeepsRawText() {
        CardEffect effect = CardEffectParser.parse("Damage:8:2");

        assertNotNull(effect);
        assertEquals("Damage:8:2", effect.getRaw());
        assertEquals("damage", effect.getType());
        assertArrayEquals(new String[]{"Damage", "8", "2"}, effect.parts());
    }

    @Test
    void skipsNullAndBlankEffects() {
        List<CardEffect> effects = CardEffectParser.parse(List.of("damage:1", " "));

        assertEquals(1, effects.size());
        assertEquals("damage", effects.get(0).getType());
        assertNull(CardEffectParser.parse((String) null));
    }

    @Test
    void knownTypesIncludePlayableAndStatusEffects() {
        assertTrue(CardEffectParser.isKnownType("damage"));
        assertTrue(CardEffectParser.isKnownType("end_turn_damage"));
        assertFalse(CardEffectParser.isKnownType("mystery_effect"));
    }

    @Test
    void registryTracksExecutionAndPreviewSupportSeparately() {
        assertTrue(CardEffectParser.isExecutionSupported("damage"));
        assertTrue(CardEffectParser.isPreviewSupported("damage"));
        assertTrue(CardEffectParser.hasExecutionHandler("damage"));
        assertTrue(CardEffectParser.hasPreviewHandler("damage"));

        assertTrue(CardEffectParser.isExecutionSupported("end_turn_damage"));
        assertFalse(CardEffectParser.isPreviewSupported("end_turn_damage"));
        assertTrue(CardEffectParser.hasExecutionHandler("end_turn_damage"));
        assertFalse(CardEffectParser.hasPreviewHandler("end_turn_damage"));

        assertFalse(CardEffectParser.isExecutionSupported("mystery_effect"));
        assertFalse(CardEffectParser.isPreviewSupported("mystery_effect"));
        assertFalse(CardEffectParser.hasExecutionHandler("mystery_effect"));
        assertFalse(CardEffectParser.hasPreviewHandler("mystery_effect"));
    }

    @Test
    void supportedEffectTypesHaveRegisteredRuntimeHandlers() {
        assertTrue(CardEffectParser.missingExecutionHandlers().isEmpty());
        assertTrue(CardEffectParser.missingPreviewHandlers().isEmpty());
    }

    @Test
    void registryValidatesDslPartCounts() {
        assertTrue(CardEffectParser.hasValidArity(CardEffectParser.parse("damage:6")));
        assertTrue(CardEffectParser.hasValidArity(CardEffectParser.parse("damage:6:2")));
        assertFalse(CardEffectParser.hasValidArity(CardEffectParser.parse("damage")));

        assertTrue(CardEffectParser.hasValidArity(CardEffectParser.parse("double_poison")));
        assertTrue(CardEffectParser.hasValidArity(CardEffectParser.parse("double_poison:3")));
        assertFalse(CardEffectParser.hasValidArity(CardEffectParser.parse("double_poison:3:extra")));

        assertEquals("2-3", CardEffectParser.expectedArity("damage"));
    }

    @Test
    void registryValidatesNumericArguments() {
        assertTrue(CardEffectParser.hasValidArgumentTypes(
                CardEffectParser.parse("damage:6:2")));
        assertFalse(CardEffectParser.hasValidArgumentTypes(
                CardEffectParser.parse("damage:abc")));

        assertTrue(CardEffectParser.hasValidArgumentTypes(
                CardEffectParser.parse("debuff:weak:2")));
        assertFalse(CardEffectParser.hasValidArgumentTypes(
                CardEffectParser.parse("debuff:weak:soon")));

        assertEquals("1,2", CardEffectParser.expectedNumericParts("damage"));
    }

    @Test
    void registryValidatesLiteralArguments() {
        assertTrue(CardEffectParser.hasValidLiteralValues(
                CardEffectParser.parse("buff:self:strength:2")));
        assertFalse(CardEffectParser.hasValidLiteralValues(
                CardEffectParser.parse("buff:enemy:strength:2")));

        assertTrue(CardEffectParser.hasValidLiteralValues(
                CardEffectParser.parse("counter:block")));
        assertFalse(CardEffectParser.hasValidLiteralValues(
                CardEffectParser.parse("counter:hp")));

        assertEquals("1=self", CardEffectParser.expectedLiteralValues("buff"));
    }
}
