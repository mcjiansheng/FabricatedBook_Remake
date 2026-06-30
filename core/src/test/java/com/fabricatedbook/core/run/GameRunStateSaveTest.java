package com.fabricatedbook.core.run;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.map.NodeEntryResolver;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRunStateSaveTest {
    @TempDir
    Path tempDir;

    @Test
    void runSaveRestoresCardsRelicsAndPotions() {
        Player player = playerWithPersistentInventory();
        player.setGold(123);
        player.setHp(57);
        player.setMaxPotionSlots(4);
        GameRunState runState = new GameRunState(777L, player);
        runState.setCurrentLayerIdx(2);

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(777L, restored.getSeed());
        assertEquals(2, restored.getCurrentLayerIdx());
        assertEquals(123, restored.getPlayer().getGold());
        assertEquals(57, restored.getPlayer().getHp());
        assertEquals(4, restored.getPlayer().getMaxPotionSlots());
        assertRestoredPersistentInventory(restored.getPlayer());
    }

    @Test
    void combatActiveNodeSaveRestoresBeforeCombatSnapshot() {
        Player player = playerWithPersistentInventory();
        player.setGold(100);
        player.setHp(70);
        GameRunState runState = new GameRunState(888L, player);
        GameRunState.NodeRef combatNode = new GameRunState.NodeRef(1, 1, 0, 1);
        runState.beginCombat(combatNode);

        player.setGold(1);
        player.takeDamage(40);
        player.getDrawPile().add(CardFactory.createFromTemplate(
                CardPool.findById("war_def1")));
        player.getRelics().clear();
        player.removePotion(0);

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(100, restored.getPlayer().getGold());
        assertEquals(70, restored.getPlayer().getHp());
        assertRestoredPersistentInventory(restored.getPlayer());
        assertNull(restored.getCompletedNode());
        assertNull(restored.getActiveNode());
        assertFalse(restored.isInCombat());
    }

    @Test
    void mapDamageModifierSurvivesRunSaveRestore() {
        GameRunState runState = new GameRunState(99L,
                new Player("p", "战士", Profession.WARRIOR));
        runState.setMapDamageModifier(2);

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(2, restored.getMapDamageModifier());
    }

    @Test
    void centralizationEntryCountSurvivesRunSaveRestore() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_centralization", player));
        GameRunState runState = new GameRunState(99L, player);
        runState.beginNode(new GameRunState.NodeRef(0, 0, 0, 1));
        new NodeEntryResolver().enterNode(runState, runState.getActiveNode());
        runState.completeActiveNode();

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(1, restored.getPlayer().getCentralizationCombatEntries());
    }

    @Test
    void activeCombatSaveRollsBackCentralizationEntryCount() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addRelic(RelicFactory.createById("relic_centralization", player));
        GameRunState runState = new GameRunState(99L, player);
        GameRunState.NodeRef fight = new GameRunState.NodeRef(0, 0, 0, 1);
        runState.beginCombat(fight);
        new NodeEntryResolver().enterNode(runState, fight);

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(0, restored.getPlayer().getCentralizationCombatEntries());
    }

    @Test
    void nonCombatActiveNodeSaveRestoresBeforeNodeEntry() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setGold(80);
        GameRunState runState = new GameRunState(123L, player);
        GameRunState.NodeRef shopNode = new GameRunState.NodeRef(1, 2, 0, 6);
        runState.beginNode(shopNode);

        new NodeEntryResolver().enterNode(runState, shopNode);
        assertTrue(player.getGold() < 80);

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(80, restored.getPlayer().getGold());
        assertNull(restored.getCompletedNode());
        assertNull(restored.getActiveNode());
    }

    @Test
    void committedNonCombatActiveNodeSaveKeepsPlayerProgress() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.setGold(80);
        GameRunState runState = new GameRunState(123L, player);
        GameRunState.NodeRef shopNode = new GameRunState.NodeRef(1, 2, 0, 6);
        runState.beginNode(shopNode);

        player.spendGold(25);
        runState.markActiveNodeProgressCommitted();

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertEquals(55, restored.getPlayer().getGold());
        assertNotNull(restored.getCompletedNode());
        assertEquals(shopNode.layer, restored.getCompletedNode().layer);
        assertEquals(shopNode.col, restored.getCompletedNode().col);
        assertEquals(shopNode.row, restored.getCompletedNode().row);
        assertEquals(shopNode.type, restored.getCompletedNode().type);
        assertNull(restored.getActiveNode());
    }

    @Test
    void committedNonCombatPotionChangeIsSaved() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        player.addPotion(new Potion("potion_test", "测试药水", "测试", List.of()));
        GameRunState runState = new GameRunState(123L, player);
        GameRunState.NodeRef eventNode = new GameRunState.NodeRef(1, 2, 0, 5);
        runState.beginNode(eventNode);

        player.removePotion(0);
        runState.markActiveNodeProgressCommitted();

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());

        assertTrue(saveManager.saveRun(runState));
        GameRunState restored = saveManager.loadRun();

        assertNotNull(restored);
        assertTrue(restored.getPlayer().getPotions().isEmpty());
        assertNotNull(restored.getCompletedNode());
        assertEquals(eventNode.type, restored.getCompletedNode().type);
        assertNull(restored.getActiveNode());
    }

    private Player playerWithPersistentInventory() {
        Player player = new Player("p", "战士", Profession.WARRIOR);
        Card upgradedAttack = CardFactory.createFromTemplate(CardPool.findById("war_atk1"));
        assertTrue(upgradedAttack.upgrade());
        player.getDrawPile().add(upgradedAttack);
        Relic relic = RelicFactory.createById("relic_bankbook", player);
        assertNotNull(relic);
        player.addRelic(relic);
        List<Potion> potions = new DataLoader().loadPotions();
        assertFalse(potions.isEmpty());
        assertTrue(player.addPotion(potions.get(0).copy()));
        return player;
    }

    private void assertRestoredPersistentInventory(Player player) {
        assertEquals(1, player.getDrawPile().size());
        assertEquals("war_atk1", player.getDrawPile().get(0).getId());
        assertTrue(player.getDrawPile().get(0).isUpgraded());
        assertTrue(player.hasRelic("relic_bankbook"));
        assertEquals(1, player.getPotions().size());
        assertEquals(new DataLoader().loadPotions().get(0).getId(),
                player.getPotions().get(0).getId());
    }
}
