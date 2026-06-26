package com.fabricatedbook.core.event;

import com.fabricatedbook.data.DataLoader;

/**
 * Resolves event option data into executable event results.
 */
public final class EventResultResolver {
    private EventResultResolver() {}

    public static EventHandler.EventResult resolve(DataLoader.EventOptionData optionData) {
        if (optionData == null || !optionData.hasExecutableResult()) {
            return null;
        }
        return new EventHandler.EventResult(optionData.getOutcomeDescription(),
                optionData.getGoldChange(), optionData.getHpChange(),
                optionData.getRelicId(), optionData.getOutcome());
    }
}
