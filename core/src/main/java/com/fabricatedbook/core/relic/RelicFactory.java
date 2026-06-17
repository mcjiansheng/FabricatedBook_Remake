package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.data.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RelicFactory — 从 JSON 数据创建可运行藏品实例。
 */
public final class RelicFactory {

    private static final Random RANDOM = new Random();

    private RelicFactory() {}

    public static List<RelicData> loadRelicData() {
        return new DataLoader().loadRelicData();
    }

    public static Relic create(RelicData data, Player owner) {
        if (data == null || owner == null) return null;
        return new DataRelic(data, owner);
    }

    public static Relic createById(String id, Player owner) {
        for (RelicData data : loadRelicData()) {
            if (data.getId().equals(id)) {
                return create(data, owner);
            }
        }
        return null;
    }

    public static Relic randomRelic(Player owner, boolean includeCursed) {
        return randomRelic(owner, includeCursed, RANDOM);
    }

    public static Relic randomRelic(Player owner, boolean includeCursed, Random random) {
        List<RelicData> candidates = new ArrayList<>();
        for (RelicData data : loadRelicData()) {
            if (includeCursed || data.getRarity() != Relic.Rarity.CURSED) {
                candidates.add(data);
            }
        }
        if (candidates.isEmpty()) return null;
        return create(candidates.get(random.nextInt(candidates.size())), owner);
    }
}
