package com.fabricatedbook.core.shop;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicData;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.data.DataLoader;

import java.util.*;

/**
 * ShopManager — 商店管理器
 * <p>
 * 管理"诡异行商"节点的商品生成、定价和购买逻辑。
 * 商品池：7 张卡牌 + 3 个藏品 + 3 个药水 + 1 个弃牌机会。
 * 定价规则参考 game_encyclopedia.md "三、节点系统 - 诡异行商"。
 * <p>
 * 引用方：ShopScreen（展示和购买商品）、CombatEngine（战斗外状态管理）
 */
public class ShopManager {

    /** 玩家引用 */
    private final Player player;

    /** 藏品管理器引用 */
    private final RelicManager relicManager;

    /** 随机数生成器 */
    private final Random random;

    /** 当前商店中的商品列表 */
    private List<ShopItem> items;

    /** 弃牌价格 */
    private int removeCost;

    /** 弃牌是否已经购买过 */
    private boolean removePurchased;

    /** 本局游戏弃牌次数 */
    private static int totalRemoveCount = 0;

    /**
     * 商店商品。
     */
    public static class ShopItem {
        public enum ItemType {
            CARD,       // 卡牌
            RELIC,      // 藏品
            POTION      // 药水
        }

        private final ItemType type;
        private final String name;
        private final String description;
        private final int price;
        private final Object data;  // Card 或 Relic 或 Potion 引用
        private boolean purchased;

        public ShopItem(ItemType type, String name, String description,
                        int price, Object data) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.price = price;
            this.data = data;
            this.purchased = false;
        }

        public ItemType getType() { return type; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public Object getData() { return data; }
        public boolean isPurchased() { return purchased; }
        public void setPurchased(boolean purchased) { this.purchased = purchased; }
    }

    /**
     * 构造商店管理器。
     *
     * @param player       玩家实体
     * @param relicManager 藏品管理器
     */
    public ShopManager(Player player, RelicManager relicManager) {
        this.player = player;
        this.relicManager = relicManager;
        this.random = new Random();
        this.items = new ArrayList<>();
        this.removeCost = 75;
        this.removePurchased = false;
    }

    /**
     * 生成商店商品。
     * <p>
     * 包括 7 张卡牌 + 3 个藏品 + 3 个药水 + 1 个弃牌机会。
     */
    public void generateItems() {
        items.clear();
        relicManager.onEnterShop();

        // 1. 7 张卡牌 — 从玩家职业的卡牌池随机选择
        String profession = player.getProfession().name().toLowerCase();
        List<Card> allCards = CardPool.getCardsByProfession(profession);
        List<Card> shopCards = CardPool.randomSelect(allCards, Math.min(7, allCards.size()));
        for (Card card : shopCards) {
            int price = card.getValue() == 1 ? 50 + random.nextInt(21)
                    : 90 + random.nextInt(21);
            items.add(new ShopItem(ShopItem.ItemType.CARD,
                    card.getName(), card.getDescription(),
                    price, card));
        }

        // 2. 3 个藏品 — 从 JSON 数据池创建真实藏品
        List<RelicData> relicPool = new DataLoader().loadRelicData().stream()
                .filter(data -> data.getRarity() != Relic.Rarity.CURSED)
                .filter(data -> !player.hasRelic(data.getId()))
                .toList();
        List<RelicData> shopRelics = randomSelect(relicPool, Math.min(3, relicPool.size()));
        for (RelicData data : shopRelics) {
            Relic relic = RelicFactory.create(data, player);
            int price = data.getRarity().getValue() * 25 + (55 + random.nextInt(16));
            items.add(new ShopItem(ShopItem.ItemType.RELIC,
                    relic.getName(), relic.getDescription(),
                    price, relic));
        }

        // 3. 3 个药水 — 从 JSON 数据池创建真实药水
        List<Potion> potionPool = new DataLoader().loadPotions();
        List<Potion> shopPotions = randomSelect(potionPool, Math.min(3, potionPool.size()));
        for (Potion potion : shopPotions) {
            int price = 30 + random.nextInt(41);
            items.add(new ShopItem(ShopItem.ItemType.POTION,
                    potion.getName(), potion.getDescription(),
                    price, potion.copy()));
        }

        // 4. 弃牌机会
        // （不添加到 items 列表，作为独立功能处理）
        this.removePurchased = false;
        this.removeCost = 75 + totalRemoveCount * 25;
    }

    /**
     * 购买指定索引的商品。
     *
     * @param index 商品索引
     * @return true 如果购买成功
     */
    public boolean purchase(int index) {
        if (index < 0 || index >= items.size()) return false;

        ShopItem item = items.get(index);
        if (item.isPurchased()) return false;
        if (player.getGold() < item.getPrice()) return false;

        // 扣除金币
        player.spendGold(item.getPrice());

        // 根据类型执行购买
        switch (item.getType()) {
            case CARD:
                // 卡牌进入玩家卡组
                Card card = (Card) item.getData();
                player.getDrawPile().add(CardFactory.createFromTemplate(card));
                player.setCardCount(player.getCardCount() + 1);
                break;

            case RELIC:
                relicManager.addRelic((Relic) item.getData());
                break;

            case POTION:
                if (!player.addPotion(((Potion) item.getData()).copy())) {
                    player.gainGold(item.getPrice());
                    return false;
                }
                break;
        }

        item.setPurchased(true);
        return true;
    }

    /**
     * 购买弃牌机会（从牌组移除一张卡）。
     *
     * @param cardIndex 要弃掉的卡牌在牌组中的索引
     * @return true 如果成功
     */
    public boolean purchaseRemove(int cardIndex) {
        if (removePurchased) return false;
        if (player.getGold() < removeCost) return false;

        // 从抽牌堆移除卡牌
        if (cardIndex >= 0 && cardIndex < player.getDrawPile().size()) {
            player.spendGold(removeCost);
            player.getDrawPile().remove(cardIndex);
            totalRemoveCount++;
            removePurchased = true;
            return true;
        }
        return false;
    }

    /**
     * 获取所有商品列表。
     *
     * @return 商品列表
     */
    public List<ShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * 获取弃牌价格。
     *
     * @return 当前弃牌价格
     */
    public int getRemoveCost() {
        return removeCost;
    }

    /**
     * 是否已购买弃牌。
     *
     * @return true 如果已购买
     */
    public boolean isRemovePurchased() {
        return removePurchased;
    }

    /**
     * 获取玩家金币数。
     *
     * @return 当前金币
     */
    public int getPlayerGold() {
        return player.getGold();
    }

    private <T> List<T> randomSelect(List<T> source, int count) {
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}
