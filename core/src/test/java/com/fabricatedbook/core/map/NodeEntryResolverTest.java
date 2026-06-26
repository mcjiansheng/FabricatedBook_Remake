package com.fabricatedbook.core.map;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.run.GameRunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeEntryResolverTest {
    @Test
    void oligarchGainsGoldOnNonCombatNodeEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_oligarch", player));
        player.setGold(10);
        GameRunState runState = new GameRunState(1L, player);

        NodeEntryResult result = new NodeEntryResolver().enterNode(runState, NodeType.SHOP);

        assertEquals(30, player.getGold());
        assertEquals(20, result.getGoldGained());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void oligarchDoesNotTriggerOnCombatNodeEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_oligarch", player));
        player.setGold(10);
        GameRunState runState = new GameRunState(1L, player);

        NodeEntryResult result = new NodeEntryResolver().enterNode(runState, NodeType.FIGHT);

        assertEquals(10, player.getGold());
        assertEquals(0, result.getGoldGained());
        assertTrue(result.getMessages().isEmpty());
    }
}
