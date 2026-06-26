package com.fabricatedbook.core.card;

import com.fabricatedbook.data.DataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardPoolTest {

    @Test
    void warriorJsonUsesRuntimeCardIds() {
        List<Card> jsonCards = new DataLoader().loadCards("warrior");
        List<String> ids = jsonCards.stream().map(Card::getId).toList();

        assertEquals(CardPool.getCardsByProfession("warrior").size(), jsonCards.size());
        assertTrue(ids.contains("war_atk1"));
        assertTrue(ids.contains("war_def1"));
        assertTrue(ids.contains("war_painful_blow"));
        assertFalse(ids.contains("warrior_attack"));
    }

    @Test
    void warriorJsonProvidesUpgradeRules() {
        Card attack = CardFactory.createFromTemplate(CardPool.findById("war_atk1"));

        assertTrue(attack.canUpgrade());
        assertTrue(attack.upgrade());
        assertEquals(List.of("damage:9"), attack.getEffects());
    }
}
