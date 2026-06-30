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
    void bankbookGainsGoldOnlyOnShopNodeEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_bankbook", player));
        player.setGold(10);
        GameRunState runState = new GameRunState(1L, player);
        NodeEntryResolver resolver = new NodeEntryResolver();

        NodeEntryResult shop = resolver.enterNode(runState, NodeType.SHOP);
        NodeEntryResult event = resolver.enterNode(runState, NodeType.UNEXPECTED);

        assertEquals(35, player.getGold());
        assertEquals(25, shop.getGoldGained());
        assertTrue(shop.getMessages().stream().anyMatch(message ->
                message.contains("捡来的存折")));
        assertEquals(0, event.getGoldGained());
    }

    @Test
    void centralizationGrowsOnlyOnCombatNodeEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_centralization", player));
        GameRunState runState = new GameRunState(1L, player);
        NodeEntryResolver resolver = new NodeEntryResolver();

        NodeEntryResult shop = resolver.enterNode(runState, NodeType.SHOP);
        NodeEntryResult fight = resolver.enterNode(runState, NodeType.FIGHT);
        resolver.enterNode(runState, NodeType.EMERGENCY);

        assertEquals(0, shop.getMessages().size());
        assertEquals(2, player.getCentralizationCombatEntries());
        assertFalse(fight.getMessages().isEmpty());
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

    @Test
    void mysticForestAdjustsMapDamageModifierWithinBounds() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        GameRunState runState = new GameRunState(1L, player);
        NodeEntryResolver resolver = new NodeEntryResolver();

        resolver.enterNode(runState, new GameRunState.NodeRef(2, 1, 0, 4));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 2, 0, 5));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 3, 0, 6));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 4, 0, 8));

        assertEquals(3, runState.getMapDamageModifier());

        resolver.enterNode(runState, new GameRunState.NodeRef(2, 1, 0, 1));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 2, 0, 2));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 3, 0, 3));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 4, 0, 1));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 5, 0, 2));
        resolver.enterNode(runState, new GameRunState.NodeRef(2, 6, 0, 3));

        assertEquals(-3, runState.getMapDamageModifier());
    }

    private Player woundedPlayer() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setHp(40);
        return player;
    }
}
