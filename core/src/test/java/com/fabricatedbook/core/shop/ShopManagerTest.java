package com.fabricatedbook.core.shop;

import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void purchaseCardAddsCopyToDeckAndMarksItemPurchased() {
        GameRunState runState = new GameRunState(10L, playerWithGold(500));
        ShopManager shop = shopFor(runState, "shop:buy-card");
        shop.generateItems();
        int cardIndex = firstItemIndex(shop, ShopManager.ShopItem.ItemType.CARD);
        ShopManager.ShopItem item = shop.getItems().get(cardIndex);
        int goldBefore = runState.getPlayer().getGold();
        int deckBefore = runState.getPlayer().getDrawPile().size();
        int cardCountBefore = runState.getPlayer().getCardCount();

        assertTrue(shop.purchase(cardIndex));

        assertEquals(goldBefore - item.getPrice(), runState.getPlayer().getGold());
        assertEquals(deckBefore + 1, runState.getPlayer().getDrawPile().size());
        assertEquals(cardCountBefore + 1, runState.getPlayer().getCardCount());
        assertTrue(item.isPurchased());
        assertFalse(shop.purchase(cardIndex));
    }

    @Test
    void purchaseRelicAddsRelicAndMarksItemPurchased() {
        GameRunState runState = new GameRunState(11L, playerWithGold(500));
        ShopManager shop = shopFor(runState, "shop:buy-relic");
        shop.generateItems();
        int relicIndex = firstItemIndex(shop, ShopManager.ShopItem.ItemType.RELIC);
        ShopManager.ShopItem item = shop.getItems().get(relicIndex);
        int goldBefore = runState.getPlayer().getGold();
        int relicsBefore = runState.getPlayer().getRelics().size();

        assertTrue(shop.purchase(relicIndex));

        assertEquals(goldBefore - item.getPrice(), runState.getPlayer().getGold());
        assertEquals(relicsBefore + 1, runState.getPlayer().getRelics().size());
        assertTrue(item.isPurchased());
        assertFalse(shop.purchase(relicIndex));
    }

    @Test
    void purchasePotionAddsCopyToPotionBarAndMarksItemPurchased() {
        GameRunState runState = new GameRunState(12L, playerWithGold(500));
        ShopManager shop = shopFor(runState, "shop:buy-potion");
        shop.generateItems();
        int potionIndex = firstItemIndex(shop, ShopManager.ShopItem.ItemType.POTION);
        ShopManager.ShopItem item = shop.getItems().get(potionIndex);
        Potion shopPotion = (Potion) item.getData();
        int goldBefore = runState.getPlayer().getGold();

        assertTrue(shop.purchase(potionIndex));

        assertEquals(goldBefore - item.getPrice(), runState.getPlayer().getGold());
        assertEquals(1, runState.getPlayer().getPotions().size());
        assertEquals(shopPotion.getId(), runState.getPlayer().getPotions().get(0).getId());
        assertTrue(item.isPurchased());
        assertFalse(shop.purchase(potionIndex));
    }

    @Test
    void purchaseFailsWithoutEnoughGoldAndLeavesItemAvailable() {
        GameRunState runState = new GameRunState(13L, playerWithGold(500));
        ShopManager shop = shopFor(runState, "shop:poor");
        shop.generateItems();
        int itemIndex = firstItemIndex(shop, ShopManager.ShopItem.ItemType.CARD);
        ShopManager.ShopItem item = shop.getItems().get(itemIndex);
        runState.getPlayer().setGold(item.getPrice() - 1);
        int deckBefore = runState.getPlayer().getDrawPile().size();

        assertFalse(shop.purchase(itemIndex));

        assertEquals(item.getPrice() - 1, runState.getPlayer().getGold());
        assertEquals(deckBefore, runState.getPlayer().getDrawPile().size());
        assertFalse(item.isPurchased());
    }

    @Test
    void fullPotionBarRefundsPotionPurchaseAndLeavesItemAvailable() {
        GameRunState runState = new GameRunState(14L, playerWithGold(500));
        fillPotionBar(runState.getPlayer());
        ShopManager shop = shopFor(runState, "shop:full-potions");
        shop.generateItems();
        int potionIndex = firstItemIndex(shop, ShopManager.ShopItem.ItemType.POTION);
        ShopManager.ShopItem item = shop.getItems().get(potionIndex);
        int goldBefore = runState.getPlayer().getGold();
        int potionsBefore = runState.getPlayer().getPotions().size();

        assertFalse(shop.purchase(potionIndex));

        assertEquals(goldBefore, runState.getPlayer().getGold());
        assertEquals(potionsBefore, runState.getPlayer().getPotions().size());
        assertFalse(item.isPurchased());
    }

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

    @Test
    void generateItemsDoesNotTriggerBankbookGold() {
        GameRunState runState = new GameRunState(4L, playerWithGold(50));
        runState.getPlayer().addRelic(RelicFactory.createById("relic_bankbook",
                runState.getPlayer()));
        ShopManager shop = shopFor(runState, "shop:bankbook");

        shop.generateItems();
        shop.generateItems();

        assertEquals(50, runState.getPlayer().getGold());
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

    private Player playerWithGold(int gold) {
        Player player = new Player("shop-test", "商店测试", Profession.WARRIOR);
        player.setGold(gold);
        return player;
    }

    private int firstItemIndex(ShopManager shop, ShopManager.ShopItem.ItemType type) {
        for (int i = 0; i < shop.getItems().size(); i++) {
            if (shop.getItems().get(i).getType() == type) {
                return i;
            }
        }
        throw new AssertionError("Shop did not generate item type: " + type);
    }

    private void fillPotionBar(Player player) {
        List<Potion> potions = new DataLoader().loadPotions();
        assertFalse(potions.isEmpty());
        for (int i = 0; i < player.getMaxPotionSlots(); i++) {
            assertTrue(player.addPotion(potions.get(i % potions.size()).copy()));
        }
    }
}
