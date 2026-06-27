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

        assertTrue(CardEffectParser.isExecutionSupported("end_turn_damage"));
        assertFalse(CardEffectParser.isPreviewSupported("end_turn_damage"));

        assertFalse(CardEffectParser.isExecutionSupported("mystery_effect"));
        assertFalse(CardEffectParser.isPreviewSupported("mystery_effect"));
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
}
