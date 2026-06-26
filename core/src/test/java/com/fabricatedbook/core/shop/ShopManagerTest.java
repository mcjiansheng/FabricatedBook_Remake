package com.fabricatedbook.core.shop;

import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.data.SaveManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void removeCardPriceIsTrackedPerRunState() {
        GameRunState firstRun = new GameRunState(1L, playerWithRemovableCard());
        ShopManager firstShop = shopFor(firstRun, "shop:first");
        firstShop.generateItems();

        assertEquals(75, firstShop.getRemoveCost());
        assertTrue(firstShop.purchaseRemove(0));
        assertEquals(1, firstRun.getShopRemoveCount());

        ShopManager laterFirstRunShop = shopFor(firstRun, "shop:later-first");
        laterFirstRunShop.generateItems();
        assertEquals(100, laterFirstRunShop.getRemoveCost());

        GameRunState secondRun = new GameRunState(2L, playerWithRemovableCard());
        ShopManager secondShop = shopFor(secondRun, "shop:second");
        secondShop.generateItems();
        assertEquals(75, secondShop.getRemoveCost());
    }

    @Test
    void removeCardCountSurvivesRunSaveRestore() {
        GameRunState runState = new GameRunState(3L, playerWithRemovableCard());
        ShopManager shop = shopFor(runState, "shop:before-save");
        shop.generateItems();
        assertTrue(shop.purchaseRemove(0));

        SaveManager saveManager = new SaveManager(tempDir.resolve("save.json").toString());
        assertTrue(saveManager.saveRun(runState));

        GameRunState restored = saveManager.loadRun();
        assertNotNull(restored);
        assertEquals(1, restored.getShopRemoveCount());

        restored.getPlayer().getDrawPile().add(CardFactory.createFromTemplate(
                CardPool.findById("war_def1")));
        ShopManager restoredShop = shopFor(restored, "shop:after-load");
        restoredShop.generateItems();
        assertEquals(100, restoredShop.getRemoveCost());
    }

    private ShopManager shopFor(GameRunState runState, String key) {
        return new ShopManager(runState.getPlayer(), new RelicManager(runState.getPlayer()),
                runState, key);
    }

    private Player playerWithRemovableCard() {
        Player player = new Player("shop-test", "商店测试", Profession.WARRIOR);
        player.setGold(500);
        player.getDrawPile().add(CardFactory.createFromTemplate(CardPool.findById("war_atk1")));
        return player;
    }
}
