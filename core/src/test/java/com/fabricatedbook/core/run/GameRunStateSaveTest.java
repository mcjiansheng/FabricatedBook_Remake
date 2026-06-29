package com.fabricatedbook.core.run;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.map.NodeEntryResolver;
import com.fabricatedbook.data.SaveManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameRunStateSaveTest {
    @TempDir
    Path tempDir;

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
}
