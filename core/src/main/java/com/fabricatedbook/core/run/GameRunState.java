package com.fabricatedbook.core.run;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.data.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Long-lived run state shared by map, combat, rewards and saves.
 */
public class GameRunState {

    private final long seed;
    private final Player player;
    private int currentLayerIdx;
    private NodeRef completedNode;
    private NodeRef activeNode;
    private PlayerSnapshot combatBaseline;

    public GameRunState(long seed, Player player) {
        this.seed = seed;
        this.player = player;
        this.currentLayerIdx = Math.max(0, player.getCurrentFloor() - 1);
    }

    public long getSeed() { return seed; }
    public Player getPlayer() { return player; }
    public int getCurrentLayerIdx() { return currentLayerIdx; }
    public void setCurrentLayerIdx(int currentLayerIdx) {
        this.currentLayerIdx = Math.max(0, Math.min(4, currentLayerIdx));
        player.setCurrentFloor(this.currentLayerIdx + 1);
    }

    public NodeRef getCompletedNode() { return completedNode; }
    public void setCompletedNode(NodeRef completedNode) { this.completedNode = completedNode; }
    public NodeRef getActiveNode() { return activeNode; }
    public void setActiveNode(NodeRef activeNode) { this.activeNode = activeNode; }
    public PlayerSnapshot getCombatBaseline() { return combatBaseline; }
    public boolean isInCombat() { return activeNode != null && combatBaseline != null; }

    public void beginCombat(NodeRef nodeRef) {
        this.activeNode = nodeRef;
        this.combatBaseline = PlayerSnapshot.from(player);
    }

    public void completeActiveNode() {
        if (activeNode != null) {
            completedNode = activeNode;
            activeNode = null;
        }
        combatBaseline = null;
    }

    public void clearCombatState() {
        activeNode = null;
        combatBaseline = null;
    }

    public Random randomFor(String key) {
        return randomFor(seed, key);
    }

    public Random randomFor(String key, Object... parts) {
        StringBuilder builder = new StringBuilder(key);
        for (Object part : parts) {
            builder.append(':').append(part);
        }
        return randomFor(builder.toString());
    }

    public static Random randomFor(long seed, String key) {
        return new Random(mix(seed, key));
    }

    public static long seedFor(long seed, String key) {
        return mix(seed, key);
    }

    private static long mix(long base, String key) {
        long h = base ^ 0x9E3779B97F4A7C15L;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0xBF58476D1CE4E5B9L;
            h ^= (h >>> 27);
        }
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h;
    }

    public static class NodeRef {
        public int layer;
        public int col;
        public int row;
        public int type;

        public NodeRef() {}

        public NodeRef(int layer, int col, int row, int type) {
            this.layer = layer;
            this.col = col;
            this.row = row;
            this.type = type;
        }
    }

    public static class PlayerSnapshot {
        public String playerId;
        public String playerName;
        public String profession;
        public int hp;
        public int maxHp;
        public int gold;
        public int currentFloor;
        public int cardCount;
        public int maxPotionSlots;
        public List<String> relicIds = new ArrayList<>();
        public List<String> potionIds = new ArrayList<>();
        public List<String> deckCardIds = new ArrayList<>();

        public static PlayerSnapshot from(Player player) {
            PlayerSnapshot snapshot = new PlayerSnapshot();
            snapshot.playerId = player.getId();
            snapshot.playerName = player.getName();
            snapshot.profession = player.getProfession().name();
            snapshot.hp = player.getHp();
            snapshot.maxHp = player.getMaxHp();
            snapshot.gold = player.getGold();
            snapshot.currentFloor = player.getCurrentFloor();
            snapshot.cardCount = player.getCardCount();
            snapshot.maxPotionSlots = player.getMaxPotionSlots();
            for (Relic relic : player.getRelics()) {
                snapshot.relicIds.add(relic.getId());
            }
            for (Potion potion : player.getPotions()) {
                snapshot.potionIds.add(potion.getId());
            }
            List<Card> cards = new ArrayList<>();
            cards.addAll(player.getDrawPile());
            cards.addAll(player.getHand());
            cards.addAll(player.getDiscardPile());
            cards.addAll(player.getExhaustPile());
            for (Card card : cards) {
                snapshot.deckCardIds.add(card.getId());
            }
            return snapshot;
        }

        public Player toPlayer() {
            Profession professionValue = Profession.valueOf(profession);
            Player restored = new Player(playerId, playerName, professionValue);
            restored.setMaxHp(maxHp);
            restored.setHp(Math.min(hp, maxHp));
            restored.setGold(gold);
            restored.setCurrentFloor(currentFloor);
            restored.setCardCount(cardCount);
            restored.setMaxPotionSlots(maxPotionSlots > 0 ? maxPotionSlots : 3);

            for (String cardId : deckCardIds) {
                Card template = CardPool.findById(cardId);
                if (template != null) {
                    restored.getDrawPile().add(CardFactory.createFromTemplate(template));
                }
            }

            for (String relicId : relicIds) {
                Relic relic = RelicFactory.createById(relicId, restored);
                if (relic != null) {
                    restored.addRelic(relic);
                }
            }
            restored.setHp(Math.min(hp, restored.getMaxHp()));

            List<Potion> potions = new DataLoader().loadPotions();
            for (String potionId : potionIds) {
                for (Potion potion : potions) {
                    if (potion.getId().equals(potionId)) {
                        restored.addPotion(potion.copy());
                        break;
                    }
                }
            }
            return restored;
        }
    }
}
