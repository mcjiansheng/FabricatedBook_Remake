package com.fabricatedbook.core.event;

import com.fabricatedbook.data.DataLoader;

import java.util.Random;

/**
 * Resolves event option data into executable event results.
 */
public final class EventResultResolver {
    private EventResultResolver() {}

    public static EventHandler.EventResult resolve(DataLoader.EventOptionData optionData) {
        return resolve(optionData, new Random(0L));
    }

    public static EventHandler.EventResult resolve(DataLoader.EventOptionData optionData,
                                                   Random random) {
        if (optionData == null || !optionData.hasExecutableResult()) {
            return null;
        }
        Random rng = random == null ? new Random() : random;
        int goldChange = resolveChange(optionData.getGoldChange(),
                optionData.getGoldChangeMin(), optionData.getGoldChangeMax(), rng);
        int hpChange = resolveChange(optionData.getHpChange(),
                optionData.getHpChangeMin(), optionData.getHpChangeMax(), rng);
        String description = formatDescription(optionData.getOutcomeDescription(),
                goldChange, hpChange);
        return new EventHandler.EventResult(description,
                goldChange, hpChange,
                optionData.getRelicId(), optionData.getOutcome(),
                optionData.isFullHeal());
    }

    private static int resolveChange(int fixedValue, Integer min, Integer max,
                                     Random random) {
        if (min == null || max == null) {
            return fixedValue;
        }
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return low + random.nextInt(high - low + 1);
    }

    private static String formatDescription(String template, int goldChange,
                                            int hpChange) {
        if (template == null || !template.contains("%d")) {
            return template;
        }
        int value = goldChange != 0 ? Math.abs(goldChange) : Math.abs(hpChange);
        return String.format(template, value);
    }
}
