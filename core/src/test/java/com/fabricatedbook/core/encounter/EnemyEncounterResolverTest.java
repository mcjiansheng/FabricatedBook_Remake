package com.fabricatedbook.core.encounter;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.data.DataLoader;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyEncounterResolverTest {

    private final DataLoader loader = new DataLoader();

    @Test
    void fifthLayerBossUsesNormalRouteByDefault() {
        Player player = player();

        EnemyEncounterResolver.EncounterResult result = EnemyEncounterResolver.resolve(
                player, loader.loadMonsters(5), 5, NodeType.BOSS, new Random(1));

        assertEquals("魔王", result.getGroup().getName());
        assertTrue(result.getEnemies().stream()
                .anyMatch(enemy -> "demon_king".equals(enemy.getId())));
    }

    @Test
    void fifthLayerBossUsesHiddenRouteWithBabelAndEncounterRelic() {
        Player player = player();
        addRelic(player, "relic_betrayal");
        addRelic(player, "relic_babel_tower");

        EnemyEncounterResolver.EncounterResult result = EnemyEncounterResolver.resolve(
                player, loader.loadMonsters(5), 5, NodeType.BOSS, new Random(1));

        assertTrue(EnemyEncounterResolver.isHiddenBossRoute(player));
        assertEquals("幕后黑手", result.getGroup().getName());
        assertTrue(result.getEnemies().stream()
                .anyMatch(enemy -> "puppet_master".equals(enemy.getId())));
    }

    @Test
    void babelTowerAddsAnEnemyToEmergencyEncounters() {
        Player plain = player();
        Player babel = player();
        addRelic(babel, "relic_babel_tower");

        EnemyEncounterResolver.EncounterResult normalResult = EnemyEncounterResolver.resolve(
                plain, loader.loadMonsters(2), 2, NodeType.EMERGENCY, new Random(1));
        EnemyEncounterResolver.EncounterResult babelResult = EnemyEncounterResolver.resolve(
                babel, loader.loadMonsters(2), 2, NodeType.EMERGENCY, new Random(1));

        assertTrue(babelResult.getEnemies().size() > normalResult.getEnemies().size());
    }

    private Player player() {
        return new Player("encounter-test", "遭遇测试战士", Profession.WARRIOR);
    }

    private void addRelic(Player player, String relicId) {
        Relic relic = RelicFactory.createById(relicId, player);
        assertTrue(relic != null);
        player.addRelic(relic);
    }
}
