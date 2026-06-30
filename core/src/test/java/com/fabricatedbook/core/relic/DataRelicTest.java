package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import org.junit.jupiter.api.Test;

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
}
