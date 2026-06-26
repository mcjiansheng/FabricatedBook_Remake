package com.fabricatedbook.core.run;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
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
}
