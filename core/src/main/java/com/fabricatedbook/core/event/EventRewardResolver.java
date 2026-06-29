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
import java.util.List;
import java.util.Random;

/**
 * Resolves special event reward placeholders into concrete rewards.
 */
public final class EventRewardResolver {
    public static final String RANDOM_LEQ3_RELIC = "relic_random_leq3";
    public static final String RANDOM_CURSE_RELIC = "relic_curse_random";
    public static final String FIVE_CARDS_REWARD = "relic_five_cards";

    private EventRewardResolver() {}

    public static EventReward applyRewards(EventHandler.EventResult result,
                                           Player owner, Random random) {
        if (result == null || result.relicId == null || result.relicId.isBlank()
                || owner == null) {
            return EventReward.empty();
        }
        Random rng = random == null ? new Random() : random;
        if (FIVE_CARDS_REWARD.equals(result.relicId)) {
            List<Card> cards = randomCards(owner, rng, 5);
            owner.getDrawPile().addAll(cards);
            return new EventReward(null, cards);
        }

        Relic relic = resolveRelic(result.relicId, owner, rng);
        if (relic != null) {
            new RelicManager(owner).addRelic(relic);
        }
        return new EventReward(relic, List.of());
    }

    public static Relic resolveRelic(String relicId, Player owner, Random random) {
        if (relicId == null || relicId.isBlank() || owner == null) {
            return null;
        }
        Random rng = random == null ? new Random() : random;
        return switch (relicId) {
            case RANDOM_LEQ3_RELIC -> randomRelic(owner, rng, false, 3);
            case RANDOM_CURSE_RELIC -> randomCursedRelic(owner, rng);
            default -> RelicFactory.createById(relicId, owner);
        };
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
                new EventReward(null, Collections.emptyList());

        private final Relic relic;
        private final List<Card> cards;

        private EventReward(Relic relic, List<Card> cards) {
            this.relic = relic;
            this.cards = List.copyOf(cards);
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
