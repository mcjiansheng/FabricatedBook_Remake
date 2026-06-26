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

    @Test
    void forestNonCombatNodeLosesGold() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setGold(50);
        GameRunState runState = new GameRunState(123L, player);
        GameRunState.NodeRef shop = new GameRunState.NodeRef(1, 2, 0, 6);

        NodeEntryResult result = new NodeEntryResolver().enterNode(runState, shop);

        assertTrue(result.getGoldLost() >= 10 && result.getGoldLost() <= 20);
        assertEquals(50 - result.getGoldLost(), player.getGold());
    }

    @Test
    void forestCombatNodeDoesNotLoseGoldOnEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setGold(50);
        GameRunState runState = new GameRunState(123L, player);
        GameRunState.NodeRef fight = new GameRunState.NodeRef(1, 2, 0, 1);

        NodeEntryResult result = new NodeEntryResolver().enterNode(runState, fight);

        assertEquals(0, result.getGoldLost());
        assertEquals(50, player.getGold());
    }

    @Test
    void mistNodeEntryIsSeedStableForSameNode() {
        GameRunState.NodeRef mistNode = new GameRunState.NodeRef(3, 4, 0, 1);
        Player firstPlayer = woundedPlayer();
        Player secondPlayer = woundedPlayer();

        NodeEntryResult first = new NodeEntryResolver()
                .enterNode(new GameRunState(456L, firstPlayer), mistNode);
        NodeEntryResult second = new NodeEntryResolver()
                .enterNode(new GameRunState(456L, secondPlayer), mistNode);

        assertTrue(first.hasChanges());
        assertEquals(first.getHpHealed(), second.getHpHealed());
        assertEquals(first.getHpLost(), second.getHpLost());
        assertEquals(firstPlayer.getHp(), secondPlayer.getHp());
    }

    private Player woundedPlayer() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setHp(40);
        return player;
    }
}
