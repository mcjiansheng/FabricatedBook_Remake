package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.entity.Enemy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataRelicTest {

    @Test
    void maxHpRelicIncreasesMaxHpAndHeals() {
        Player player = player();
        player.takeDamage(20);
        int maxHpBefore = player.getMaxHp();
        int hpBefore = player.getHp();

        addRelic(player, "relic_hot_water_flask");

        assertEquals(maxHpBefore + 5, player.getMaxHp());
        assertEquals(hpBefore + 5, player.getHp());
        assertTrue(player.hasRelic("relic_hot_water_flask"));
    }

    @Test
    void immediateGoldRelicGrantsGoldOnPickup() {
        Player player = player();
        int goldBefore = player.getGold();

        addRelic(player, "relic_old_coin");

        assertEquals(goldBefore + 30, player.getGold());
        assertTrue(player.hasRelic("relic_old_coin"));
    }

    @Test
    void immediateCardRelicAddsCardAndCardCount() {
        Player player = player();
        int deckBefore = player.getDrawPile().size();
        int cardCountBefore = player.getCardCount();

        addRelic(player, "relic_speech_draft");

        assertEquals(deckBefore + 1, player.getDrawPile().size());
        assertEquals(cardCountBefore + 1, player.getCardCount());
        assertTrue(player.hasRelic("relic_speech_draft"));
    }

    @Test
    void cursedHumilityReducesMaxHpAndClampsCurrentHp() {
        Player player = player();
        int maxHpBefore = player.getMaxHp();

        addRelic(player, "relic_humility");

        assertEquals(maxHpBefore - 24, player.getMaxHp());
        assertEquals(player.getMaxHp(), player.getHp());
        assertTrue(player.hasRelic("relic_humility"));
    }

    @Test
    void centralizationDamageScalesWithCombatEntryCount() {
        Player player = player();
        addRelic(player, "relic_centralization");
        RelicManager relicManager = new RelicManager(player);
        Enemy enemy = new Enemy("dummy", "测试敌人", 40, java.util.List.of("atk1"));

        assertEquals(100, relicManager.modifyDamage(100, player, enemy));
        player.setCentralizationCombatEntries(3);

        assertEquals(115, relicManager.modifyDamage(100, player, enemy));
        assertEquals(115, relicManager.previewModifyDamage(100, player, enemy));
    }

    @Test
    void betrayalAndHatredModifyFifthFloorEnemyHpAtCombatStart() {
        Player betrayalPlayer = player();
        betrayalPlayer.setCurrentFloor(5);
        addRelic(betrayalPlayer, "relic_betrayal");
        Enemy strongerEnemy = new Enemy("enemy", "测试敌人", 100, java.util.List.of("atk1"));
        new RelicManager(betrayalPlayer).modifyEnemiesAtCombatStart(
                java.util.List.of(strongerEnemy));

        assertEquals(120, strongerEnemy.getMaxHp());
        assertEquals(120, strongerEnemy.getHp());

        Player hatredPlayer = player();
        hatredPlayer.setCurrentFloor(5);
        addRelic(hatredPlayer, "relic_hatred");
        Enemy weakerEnemy = new Enemy("enemy", "测试敌人", 100, java.util.List.of("atk1"));
        new RelicManager(hatredPlayer).modifyEnemiesAtCombatStart(
                java.util.List.of(weakerEnemy));

        assertEquals(80, weakerEnemy.getMaxHp());
        assertEquals(80, weakerEnemy.getHp());
    }

    @Test
    void betrayalAndHatredDoNotModifyEnemyHpBeforeFifthFloor() {
        Player player = player();
        player.setCurrentFloor(4);
        addRelic(player, "relic_betrayal");
        Enemy enemy = new Enemy("enemy", "测试敌人", 100, java.util.List.of("atk1"));

        new RelicManager(player).modifyEnemiesAtCombatStart(java.util.List.of(enemy));

        assertEquals(100, enemy.getMaxHp());
        assertEquals(100, enemy.getHp());
    }

    @Test
    void avengerDamageBonusUsesOneThirdChance() {
        Player player = player();
        Enemy enemy = new Enemy("enemy", "测试敌人", 100, List.of("atk1"));

        DataRelic procAtUpperBound = new DataRelic(
                relicData("relic_avenger"), player, new FixedNextIntRandom(32));
        DataRelic missAtLowerBound = new DataRelic(
                relicData("relic_avenger"), player, new FixedNextIntRandom(33));

        assertEquals(130, procAtUpperBound.modifyOutgoingDamage(100, enemy));
        assertEquals(100, missAtLowerBound.modifyOutgoingDamage(100, enemy));
        assertEquals(100, procAtUpperBound.previewOutgoingDamage(100, enemy));
    }

    private Player player() {
        Player player = new Player("relic-test", "藏品测试", Profession.WARRIOR);
        player.setGold(0);
        return player;
    }

    private void addRelic(Player player, String relicId) {
        Relic relic = RelicFactory.createById(relicId, player);
        assertNotNull(relic);
        new RelicManager(player).addRelic(relic);
    }

    private RelicData relicData(String relicId) {
        for (RelicData data : RelicFactory.loadRelicData()) {
            if (data.getId().equals(relicId)) {
                return data;
            }
        }
        throw new AssertionError("Missing relic data: " + relicId);
    }

    private static class FixedNextIntRandom extends Random {
        private final int value;

        FixedNextIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
