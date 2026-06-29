package com.fabricatedbook.core.event;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicData;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Resolves special event reward placeholders into concrete rewards.
 */
public final class EventRewardResolver {
    public static final String RANDOM_LEQ3_RELIC = "relic_random_leq3";
    public static final String RANDOM_CURSE_RELIC = "relic_curse_random";
    public static final String FIVE_CARDS_REWARD = "relic_five_cards";
    public static final String NUKE_REWARD = "relic_nuke";
    private static final Map<String, SpecialRewardExecutor> SPECIAL_REWARD_EXECUTORS =
            createSpecialRewardExecutors();

    private EventRewardResolver() {}

    public static EventReward applyRewards(EventHandler.EventResult result,
                                           Player owner, Random random) {
        if (result == null || result.relicId == null || result.relicId.isBlank()
                || owner == null) {
            return EventReward.empty();
        }
        RewardContext context = new RewardContext(owner,
                random == null ? new Random() : random);
        SpecialRewardExecutor executor = SPECIAL_REWARD_EXECUTORS.get(result.relicId);
        if (executor != null) {
            return executor.apply(context);
        }

        Relic relic = RelicFactory.createById(result.relicId, owner);
        if (relic != null) {
            new RelicManager(owner).addRelic(relic);
        }
        return new EventReward(relic, List.of(), null);
    }

    public static Relic resolveRelic(String relicId, Player owner, Random random) {
        if (relicId == null || relicId.isBlank() || owner == null) {
            return null;
        }
        RewardContext context = new RewardContext(owner,
                random == null ? new Random() : random);
        SpecialRewardExecutor executor = SPECIAL_REWARD_EXECUTORS.get(relicId);
        if (executor != null) {
            return executor.resolveRelic(context);
        }
        return RelicFactory.createById(relicId, owner);
    }

    public static boolean isSpecialRewardId(String rewardId) {
        return rewardId != null && SPECIAL_REWARD_EXECUTORS.containsKey(rewardId);
    }

    public static List<Card> randomCards(Player owner, Random random, int count) {
        if (owner == null || count <= 0) {
            return List.of();
        }
        Random rng = random == null ? new Random() : random;
        String profession = owner.getProfession() == null
                ? null : owner.getProfession().name().toLowerCase();
        List<Card> pool = CardPool.getObtainableCardsByProfession(profession);
        if (pool.isEmpty()) {
            return List.of();
        }
        List<Card> cards = new ArrayList<>();
        List<Card> selected = CardPool.randomSelect(pool, count, rng);
        for (Card template : selected) {
            Card card = CardFactory.createFromTemplate(template);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    private static Relic randomRelic(Player owner, Random random,
                                     boolean includeCursed, int maxRarityValue) {
        List<RelicData> candidates = new ArrayList<>();
        for (RelicData data : RelicFactory.loadRelicData()) {
            if (data.getRarity() == null) {
                continue;
            }
            if (!includeCursed && data.getRarity() == Relic.Rarity.CURSED) {
                continue;
            }
            if (data.getRarity() == Relic.Rarity.SPECIAL) {
                continue;
            }
            if (data.getRarity().getValue() <= maxRarityValue) {
                candidates.add(data);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return RelicFactory.create(candidates.get(random.nextInt(candidates.size())), owner);
    }

    private static Map<String, SpecialRewardExecutor> createSpecialRewardExecutors() {
        Map<String, SpecialRewardExecutor> executors = new HashMap<>();
        executors.put(RANDOM_LEQ3_RELIC, new SpecialRewardExecutor() {
            @Override
            public EventReward apply(RewardContext context) {
                Relic relic = randomRelic(context.owner(), context.random(), false, 3);
                addRelic(context.owner(), relic);
                return new EventReward(relic, List.of(), null);
            }

            @Override
            public Relic resolveRelic(RewardContext context) {
                return randomRelic(context.owner(), context.random(), false, 3);
            }
        });
        executors.put(RANDOM_CURSE_RELIC, new SpecialRewardExecutor() {
            @Override
            public EventReward apply(RewardContext context) {
                Relic relic = randomCursedRelic(context.owner(), context.random());
                addRelic(context.owner(), relic);
                return new EventReward(relic, List.of(), null);
            }

            @Override
            public Relic resolveRelic(RewardContext context) {
                return randomCursedRelic(context.owner(), context.random());
            }
        });
        executors.put(FIVE_CARDS_REWARD, context -> {
            List<Card> cards = randomCards(context.owner(), context.random(), 5);
            context.owner().getDrawPile().addAll(cards);
            return new EventReward(null, cards, null);
        });
        executors.put(NUKE_REWARD, context ->
                new EventReward(null, List.of(), NUKE_REWARD));
        return Map.copyOf(executors);
    }

    private static void addRelic(Player owner, Relic relic) {
        if (owner != null && relic != null) {
            new RelicManager(owner).addRelic(relic);
        }
    }

    private interface SpecialRewardExecutor {
        EventReward apply(RewardContext context);

        default Relic resolveRelic(RewardContext context) {
            return null;
        }
    }

    private record RewardContext(Player owner, Random random) {}

    private static Relic randomCursedRelic(Player owner, Random random) {
        List<RelicData> candidates = new ArrayList<>();
        for (RelicData data : RelicFactory.loadRelicData()) {
            if (data.getRarity() == Relic.Rarity.CURSED) {
                candidates.add(data);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return RelicFactory.create(candidates.get(random.nextInt(candidates.size())), owner);
    }

    public static final class EventReward {
        private static final EventReward EMPTY =
                new EventReward(null, Collections.emptyList(), null);

        private final Relic relic;
        private final List<Card> cards;
        private final String unresolvedSpecialRewardId;

        private EventReward(Relic relic, List<Card> cards,
                            String unresolvedSpecialRewardId) {
            this.relic = relic;
            this.cards = List.copyOf(cards);
            this.unresolvedSpecialRewardId = unresolvedSpecialRewardId;
        }

        public static EventReward empty() {
            return EMPTY;
        }

        public Relic getRelic() {
            return relic;
        }

        public List<Card> getCards() {
            return cards;
        }

        public String getUnresolvedSpecialRewardId() {
            return unresolvedSpecialRewardId;
        }

        public String getRelicName() {
            return relic == null ? null : relic.getName();
        }

        public String cardNames() {
            if (cards.isEmpty()) {
                return "";
            }
            List<String> names = new ArrayList<>();
            for (Card card : cards) {
                names.add(card.getName());
            }
            return String.join("、", names);
        }
    }
}
