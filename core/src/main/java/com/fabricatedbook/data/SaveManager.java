package com.fabricatedbook.data;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SaveManager — 存档管理器
 * <p>
 * 负责玩家存档的序列化与反序列化。
 * 将 Player 状态（生命值、金币、卡牌、藏品等）保存为 JSON 文件，
 * 并可从 JSON 文件恢复玩家状态。
 * <p>
 * 引用方：GameController（存档/读档时调用）、TitleScreen（继续游戏按钮）
 */
public class SaveManager {

    /** 存档文件路径 */
    private static final String SAVE_FILE = "saves/save.json";

    /** Gson 实例 */
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create();

    /**
     * 存档数据载体（用于 JSON 序列化/反序列化）。
     */
    public static class SaveData {
        public String playerId;
        public String playerName;
        public String profession;
        public int hp;
        public int maxHp;
        public int gold;
        public int currentFloor;
        public int cardCount;
        public List<String> relicIds;
        public List<String> potionIds;
        public List<SerializableCard> deck;

        /** 无参构造（Gson 反序列化需要） */
        public SaveData() {
            this.relicIds = new ArrayList<>();
            this.potionIds = new ArrayList<>();
            this.deck = new ArrayList<>();
        }
    }

    /**
     * 可序列化的卡牌数据载体。
     */
    public static class SerializableCard {
        public String id;
        public String name;
        public int cost;
        public String description;
        public String type;
        public String rarity;
        public int value;
        public String targetType;
        public boolean exhaust;
        public boolean retain;
        public boolean ethereal;

        public SerializableCard() {}

        public SerializableCard(Card card) {
            this.id = card.getId();
            this.name = card.getName();
            this.cost = card.getCost();
            this.description = card.getDescription();
            this.type = card.getType().name();
            this.rarity = card.getRarity().name();
            this.value = card.getValue();
            this.targetType = card.getTargetType().name();
            this.exhaust = card.isExhaust();
            this.retain = card.isRetain();
            this.ethereal = card.isEthereal();
        }
    }

    /**
     * 保存玩家状态到存档文件。
     *
     * @param player 玩家实体
     * @return true 如果保存成功
     */
    public boolean save(Player player) {
        try {
            SaveData data = new SaveData();
            data.playerId = player.getId();
            data.playerName = player.getName();
            data.profession = player.getProfession().name();
            data.hp = player.getHp();
            data.maxHp = player.getMaxHp();
            data.gold = player.getGold();
            data.currentFloor = player.getCurrentFloor();
            data.cardCount = player.getCardCount();

            // 保存藏品 ID
            for (Relic relic : player.getRelics()) {
                data.relicIds.add(relic.getId());
            }

            for (Potion potion : player.getPotions()) {
                data.potionIds.add(potion.getId());
            }

            // 保存卡牌组（从 drawPile + hand + discardPile + exhaustPile）
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(player.getDrawPile());
            allCards.addAll(player.getHand());
            allCards.addAll(player.getDiscardPile());
            allCards.addAll(player.getExhaustPile());
            for (Card card : allCards) {
                data.deck.add(new SerializableCard(card));
            }

            // 确保 saves 目录存在
            File saveDir = new File("saves");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // 写入 JSON 文件
            String json = gson.toJson(data);
            try (FileWriter writer = new FileWriter(SAVE_FILE,
                    StandardCharsets.UTF_8)) {
                writer.write(json);
            }

            System.out.println("[SaveManager] 存档成功: " + SAVE_FILE);
            return true;

        } catch (Exception e) {
            System.err.println("[SaveManager] 存档失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从存档文件读取玩家状态。
     *
     * @return 读取成功的玩家实体，失败返回 null
     */
    public Player load() {
        try {
            File file = new File(SAVE_FILE);
            if (!file.exists()) {
                System.out.println("[SaveManager] 存档文件不存在: " + SAVE_FILE);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                char[] buf = new char[4096];
                int len;
                while ((len = reader.read(buf)) != -1) {
                    sb.append(buf, 0, len);
                }
            }

            SaveData data = gson.fromJson(sb.toString(), SaveData.class);
            if (data == null) {
                System.out.println("[SaveManager] 存档数据为空");
                return null;
            }

            // 恢复玩家
            Profession profession = Profession.valueOf(data.profession);
            Player player = new Player(data.playerId, data.playerName, profession);
            player.setMaxHp(data.maxHp);
            player.setHp(Math.min(data.hp, data.maxHp));
            player.setGold(data.gold);
            player.setCurrentFloor(data.currentFloor);
            player.setCardCount(data.cardCount);

            // 恢复卡牌组到抽牌堆
            for (SerializableCard sc : data.deck) {
                Card template = CardPool.findById(sc.id);
                if (template != null) {
                    player.getDrawPile().add(CardFactory.createFromTemplate(template));
                } else {
                    Card.CardType type = Card.CardType.valueOf(sc.type);
                    Card.Rarity rarity = Card.Rarity.valueOf(sc.rarity);
                    Card.TargetType targetType = Card.TargetType.valueOf(sc.targetType);
                    Card card = new Card(sc.id, sc.name, sc.cost, sc.description,
                            type, rarity, sc.value, targetType, 1,
                            new ArrayList<>(), sc.exhaust, sc.retain, sc.ethereal,
                            data.profession.toLowerCase());
                    player.getDrawPile().add(card);
                }
            }

            if (data.relicIds != null) {
                for (String relicId : data.relicIds) {
                    Relic relic = RelicFactory.createById(relicId, player);
                    if (relic != null) {
                        player.addRelic(relic);
                    }
                }
                player.setHp(Math.min(data.hp, player.getMaxHp()));
            }

            if (data.potionIds != null) {
                List<Potion> potions = new DataLoader().loadPotions();
                for (String potionId : data.potionIds) {
                    for (Potion potion : potions) {
                        if (potion.getId().equals(potionId)) {
                            player.addPotion(potion.copy());
                            break;
                        }
                    }
                }
            }

            System.out.println("[SaveManager] 读档成功: " + SAVE_FILE);
            return player;

        } catch (Exception e) {
            System.err.println("[SaveManager] 读档失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查存档文件是否存在。
     *
     * @return true 如果存档存在
     */
    public boolean hasSave() {
        return new File(SAVE_FILE).exists();
    }

    /**
     * 删除存档文件。
     *
     * @return true 如果删除成功
     */
    public boolean deleteSave() {
        File file = new File(SAVE_FILE);
        return file.exists() && file.delete();
    }
}
