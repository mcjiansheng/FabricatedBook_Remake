package com.fabricatedbook.data;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.run.GameRunState;
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

    /** 默认存档文件路径 */
    private static final String DEFAULT_SAVE_FILE = "saves/save.json";

    private final String saveFile;

    /** Gson 实例 */
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create();

    /**
     * 存档数据载体（用于 JSON 序列化/反序列化）。
     */
    public static class SaveData {
        public int version;
        public long runSeed;
        public int currentLayerIdx;
        public GameRunState.NodeRef completedNode;
        public GameRunState.NodeRef activeNode;
        public GameRunState.PlayerSnapshot combatBaseline;
        public int shopRemoveCount;
        public int mapDamageModifier;
        public String playerId;
        public String playerName;
        public String profession;
        public int hp;
        public int maxHp;
        public int gold;
        public int currentFloor;
        public int cardCount;
        public int centralizationCombatEntries;
        public int maxPotionSlots;
        public List<String> relicIds;
        public List<String> potionIds;
        public List<SerializableCard> deck;

        /** 无参构造（Gson 反序列化需要） */
        public SaveData() {
            this.version = 1;
            this.relicIds = new ArrayList<>();
            this.potionIds = new ArrayList<>();
            this.deck = new ArrayList<>();
        }
    }

    public SaveManager() {
        this(DEFAULT_SAVE_FILE);
    }

    public SaveManager(String saveFile) {
        this.saveFile = saveFile == null || saveFile.isBlank()
                ? DEFAULT_SAVE_FILE
                : saveFile;
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
        public boolean unplayable;
        public boolean upgraded;

        public SerializableCard() {}

        public SerializableCard(Card card) {
            this.id = card.getId();
            this.name = card.getBaseName();
            this.cost = card.getCost();
            this.description = card.getDescription();
            this.type = card.getType().name();
            this.rarity = card.getRarity().name();
            this.value = card.getValue();
            this.targetType = card.getTargetType().name();
            this.exhaust = card.isExhaust();
            this.retain = card.isRetain();
            this.ethereal = card.isEthereal();
            this.unplayable = card.isUnplayable();
            this.upgraded = card.isUpgraded();
        }
    }

    /**
     * 保存玩家状态到存档文件。
     *
     * @param player 玩家实体
     * @return true 如果保存成功
     */
    public boolean save(Player player) {
        return saveSnapshot(GameRunState.PlayerSnapshot.from(player), 0L,
                Math.max(0, player.getCurrentFloor() - 1), null, null, null, 0, 0);
    }

    public boolean saveRun(GameRunState runState) {
        if (runState == null || runState.getPlayer() == null) {
            return false;
        }
        boolean activeNodeCommitted = runState.isNodeActive()
                && runState.isActiveNodeProgressCommitted();
        GameRunState.PlayerSnapshot playerSnapshot = runState.isNodeActive()
                && !activeNodeCommitted
                ? runState.getCombatBaseline()
                : GameRunState.PlayerSnapshot.from(runState.getPlayer());
        GameRunState.NodeRef completedNode = activeNodeCommitted
                ? runState.getActiveNode()
                : runState.getCompletedNode();
        GameRunState.NodeRef activeNode = runState.isNodeActive() && !activeNodeCommitted
                ? runState.getActiveNode()
                : null;
        return saveSnapshot(playerSnapshot, runState.getSeed(),
                runState.getCurrentLayerIdx(), completedNode,
                activeNode, runState.getCombatBaseline(), runState.getShopRemoveCount(),
                runState.getMapDamageModifier());
    }

    private boolean saveSnapshot(GameRunState.PlayerSnapshot snapshot, long seed,
                                 int currentLayerIdx,
                                 GameRunState.NodeRef completedNode,
                                 GameRunState.NodeRef activeNode,
                                 GameRunState.PlayerSnapshot combatBaseline,
                                 int shopRemoveCount,
                                 int mapDamageModifier) {
        try {
            SaveData data = new SaveData();
            data.version = 3;
            data.runSeed = seed;
            data.currentLayerIdx = currentLayerIdx;
            data.completedNode = completedNode;
            data.activeNode = activeNode;
            data.combatBaseline = combatBaseline;
            data.shopRemoveCount = Math.max(0, shopRemoveCount);
            data.mapDamageModifier = Math.max(-3, Math.min(3, mapDamageModifier));
            data.playerId = snapshot.playerId;
            data.playerName = snapshot.playerName;
            data.profession = snapshot.profession;
            data.hp = snapshot.hp;
            data.maxHp = snapshot.maxHp;
            data.gold = snapshot.gold;
            data.currentFloor = snapshot.currentFloor;
            data.cardCount = snapshot.cardCount;
            data.centralizationCombatEntries = Math.max(0,
                    snapshot.centralizationCombatEntries);
            data.maxPotionSlots = snapshot.maxPotionSlots;
            data.relicIds.addAll(snapshot.relicIds);
            data.potionIds.addAll(snapshot.potionIds);
            for (int i = 0; i < snapshot.deckCardIds.size(); i++) {
                String cardId = snapshot.deckCardIds.get(i);
                Card template = CardPool.findById(cardId);
                if (template != null) {
                    Card card = CardFactory.createFromTemplate(template);
                    if (snapshot.deckCardUpgraded != null
                            && i < snapshot.deckCardUpgraded.size()
                            && Boolean.TRUE.equals(snapshot.deckCardUpgraded.get(i))) {
                        card.upgrade();
                    }
                    data.deck.add(new SerializableCard(card));
                }
            }

            // 确保 saves 目录存在
            File saveTarget = new File(saveFile);
            File saveDir = saveTarget.getParentFile();
            if (saveDir != null && !saveDir.exists()) {
                saveDir.mkdirs();
            }

            // 写入 JSON 文件
            String json = gson.toJson(data);
            try (FileWriter writer = new FileWriter(saveTarget,
                    StandardCharsets.UTF_8)) {
                writer.write(json);
            }

            System.out.println("[SaveManager] 存档成功: " + saveFile);
            return true;

        } catch (Exception e) {
            System.err.println("[SaveManager] 存档失败: " + e.getMessage());
            return false;
        }
    }

    public GameRunState loadRun() {
        SaveData data = loadData();
        if (data == null) {
            return null;
        }
        Player player = playerFromData(data);
        if (player == null) {
            return null;
        }
        long seed = data.runSeed != 0L ? data.runSeed : System.currentTimeMillis();
        GameRunState runState = new GameRunState(seed, player);
        runState.setCurrentLayerIdx(data.version >= 2
                ? data.currentLayerIdx
                : Math.max(0, data.currentFloor - 1));
        runState.setShopRemoveCount(data.version >= 2 ? data.shopRemoveCount : 0);
        runState.setMapDamageModifier(data.version >= 2 ? data.mapDamageModifier : 0);
        runState.setCompletedNode(data.completedNode);
        runState.clearCombatState();
        System.out.println("[SaveManager] 对局读档成功: " + saveFile);
        return runState;
    }

    /**
     * 从存档文件读取玩家状态。
     *
     * @return 读取成功的玩家实体，失败返回 null
     */
    public Player load() {
        SaveData data = loadData();
        return data == null ? null : playerFromData(data);
    }

    private SaveData loadData() {
        try {
            File file = new File(saveFile);
            if (!file.exists()) {
                System.out.println("[SaveManager] 存档文件不存在: " + saveFile);
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
            return data;

        } catch (Exception e) {
            System.err.println("[SaveManager] 读档失败: " + e.getMessage());
            return null;
        }
    }

    private Player playerFromData(SaveData data) {
        try {
            Profession profession = Profession.valueOf(data.profession);
            Player player = new Player(data.playerId, data.playerName, profession);
            player.setMaxHp(data.maxHp);
            player.setHp(Math.min(data.hp, data.maxHp));
            player.setGold(data.gold);
            player.setCurrentFloor(data.currentFloor);
            player.setCardCount(data.cardCount);
            player.setCentralizationCombatEntries(data.version >= 3
                    ? data.centralizationCombatEntries : 0);
            player.setMaxPotionSlots(data.maxPotionSlots > 0 ? data.maxPotionSlots : 3);

            // 恢复卡牌组到抽牌堆
            for (SerializableCard sc : data.deck) {
                Card template = CardPool.findById(sc.id);
                if (template != null) {
                    Card card = CardFactory.createFromTemplate(template);
                    if (sc.upgraded) {
                        card.upgrade();
                    }
                    player.getDrawPile().add(card);
                } else {
                    Card.CardType type = Card.CardType.valueOf(sc.type);
                    Card.Rarity rarity = Card.Rarity.valueOf(sc.rarity);
                    Card.TargetType targetType = Card.TargetType.valueOf(sc.targetType);
                    Card card = new Card(sc.id, sc.name, sc.cost, sc.description,
                            type, rarity, sc.value, targetType, 1,
                            new ArrayList<>(), sc.exhaust, sc.retain, sc.ethereal,
                            sc.unplayable, data.profession.toLowerCase());
                    card.setUpgraded(sc.upgraded);
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

            System.out.println("[SaveManager] 读档成功: " + saveFile);
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
        return new File(saveFile).exists();
    }

    /**
     * 删除存档文件。
     *
     * @return true 如果删除成功
     */
    public boolean deleteSave() {
        File file = new File(saveFile);
        return file.exists() && file.delete();
    }
}
