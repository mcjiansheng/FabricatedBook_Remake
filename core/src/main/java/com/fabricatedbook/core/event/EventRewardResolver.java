package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicData;
import com.fabricatedbook.core.relic.RelicFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolves special event reward placeholders into concrete rewards.
 */
public final class EventRewardResolver {
    public static final String RANDOM_LEQ3_RELIC = "relic_random_leq3";
    public static final String RANDOM_CURSE_RELIC = "relic_curse_random";

    private EventRewardResolver() {}

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
}
