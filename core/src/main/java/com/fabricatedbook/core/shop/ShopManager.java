package com.fabricatedbook.core.shop;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicManager;

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

        // 2. 3 个藏品 — 随机生成（简化：使用名称占位）
        // 实际藏品通过 JSON 配置或硬编码列表获取
        String[] relicNames = {"古旧钱币", "热水壶", "靶子", "石像鬼塑像",
                "微缩舞台模型", "道具箱"};
        for (int i = 0; i < 3; i++) {
            String relicName = relicNames[random.nextInt(relicNames.length)];
            int relicValue = switch (relicName) {
                case "古旧钱币", "热水壶", "靶子" -> 0;
                case "石像鬼塑像" -> 1;
                case "微缩舞台模型", "道具箱" -> 2;
                default -> 1;
            };
            int price = relicValue * 25 + (55 + random.nextInt(16));
            items.add(new ShopItem(ShopItem.ItemType.RELIC,
                    relicName, "藏品效果描述",
                    price, relicName));
        }

        // 3. 3 个药水
        String[] potionNames = {"回血药水", "护盾药水", "攻击药水",
                "能量药水", "力量药水", "中毒药水"};
        for (int i = 0; i < 3; i++) {
            String potionName = potionNames[random.nextInt(potionNames.length)];
            int price = 30 + random.nextInt(41);
            items.add(new ShopItem(ShopItem.ItemType.POTION,
                    potionName, "药水效果描述",
                    price, potionName));
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
                player.getDrawPile().add(new Card(card.getId(), card.getName(),
                        card.getCost(), card.getDescription(), card.getType(),
                        card.getRarity(), card.getValue(), card.getTargetType(),
                        card.getTargetCount(), new ArrayList<>(card.getEffects()),
                        card.isExhaust(), card.getProfession()));
                player.setCardCount(player.getCardCount() + 1);
                break;

            case RELIC:
                // 藏品通过 RelicManager 添加
                // 简化：记录购买
                System.out.println("[Shop] 购买藏品: " + item.getName());
                break;

            case POTION:
                // 药水（暂未实现药水系统）
                System.out.println("[Shop] 购买药水: " + item.getName());
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
}
