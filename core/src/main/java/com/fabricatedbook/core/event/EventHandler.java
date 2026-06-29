package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.data.DataLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * EventHandler dispatches data-driven event options and explicitly registered
 * Java executors.
 */
public class EventHandler {

    private final Random random;
    private final Map<String, DataLoader.EventData> eventsByName;
    private final Map<String, JavaEventExecutor> javaExecutors;

    @FunctionalInterface
    public interface EventCallback {
        void onResult(String description, int goldChange,
                      int hpChange, String relicId);
    }

    public static class EventOption {
        public final String label;
        public final String description;

        public EventOption(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    public static class EventResult {
        public final String description;
        public final int goldChange;
        public final int hpChange;
        public final String relicId;
        public final String outcome;
        public final boolean fullHeal;

        public EventResult(String description, int goldChange,
                           int hpChange, String relicId) {
            this(description, goldChange, hpChange, relicId, null);
        }

        public EventResult(String description, int goldChange,
                           int hpChange, String relicId, String outcome) {
            this(description, goldChange, hpChange, relicId, outcome, false);
        }

        public EventResult(String description, int goldChange,
                           int hpChange, String relicId, String outcome,
                           boolean fullHeal) {
            this.description = description;
            this.goldChange = goldChange;
            this.hpChange = hpChange;
            this.relicId = relicId;
            this.outcome = outcome;
            this.fullHeal = fullHeal;
        }
    }

    public EventHandler() {
        this(new Random());
    }

    public EventHandler(Random random) {
        this.random = random == null ? new Random() : random;
        this.eventsByName = loadEventsByName();
        this.javaExecutors = createJavaExecutors();
    }

    public List<String> getEventNames() {
        List<String> names = new ArrayList<>();
        for (DataLoader.EventData eventData : eventsByName.values()) {
            if (eventData.isRandomPool()) {
                names.add(eventData.getName());
            }
        }
        return names;
    }

    public String getEventDescription(String eventName) {
        DataLoader.EventData eventData = eventsByName.get(eventName);
        if (eventData != null && eventData.getDescription() != null
                && !eventData.getDescription().isBlank()) {
            return eventData.getDescription();
        }
        return "无事发生。";
    }

    public EventResult executeEvent(String eventName, int optionIndex) {
        return executeEvent(eventName, optionIndex, null);
    }

    public EventResult executeEvent(String eventName, int optionIndex, Player player) {
        DataExecution dataExecution = executeDataEvent(eventName, optionIndex, player);
        if (dataExecution.handled()) {
            return dataExecution.result();
        }
        JavaEventExecutor executor = javaExecutors.get(eventName);
        if (executor != null) {
            return executor.execute(optionIndex, player);
        }
        return new EventResult("无事发生。", 0, 0, null);
    }

    public List<EventOption> getOptions(String eventName) {
        return getOptions(eventName, null);
    }

    public List<EventOption> getOptions(String eventName, Player player) {
        List<EventOption> dataOptions = dataOptions(eventName, player);
        if (!dataOptions.isEmpty()) {
            return dataOptions;
        }
        return List.of(new EventOption("离开", "无事发生"));
    }

    private Map<String, DataLoader.EventData> loadEventsByName() {
        Map<String, DataLoader.EventData> events = new LinkedHashMap<>();
        for (DataLoader.EventData eventData : new DataLoader().loadEvents()) {
            if (eventData.getName() != null && !eventData.getName().isBlank()) {
                events.put(eventData.getName(), eventData);
            }
        }
        return events;
    }

    private List<EventOption> dataOptions(String eventName, Player player) {
        List<DataLoader.EventOptionData> options = visibleOptionData(eventName, player);
        if (options.isEmpty()) {
            return List.of();
        }
        List<EventOption> eventOptions = new ArrayList<>();
        for (DataLoader.EventOptionData optionData : options) {
            eventOptions.add(new EventOption(optionData.getText(),
                    optionData.getResult()));
        }
        return eventOptions;
    }

    private boolean optionConditionMatches(DataLoader.EventOptionData optionData,
                                           Player player) {
        List<String> requiredRelics = optionData.getRequiresAnyRelic();
        if (requiredRelics.isEmpty()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        for (String relicId : requiredRelics) {
            if (player.hasRelic(relicId)) {
                return true;
            }
        }
        return false;
    }

    private DataExecution executeDataEvent(String eventName, int optionIndex,
                                           Player player) {
        List<DataLoader.EventOptionData> options = visibleOptionData(eventName, player);
        if (optionIndex < 0 || optionIndex >= options.size()) {
            if (eventsByName.containsKey(eventName)) {
                return DataExecution.handled(
                        new EventResult("你没有作出选择。", 0, 0, null));
            }
            return DataExecution.unhandled();
        }
        DataLoader.EventOptionData optionData = options.get(optionIndex);
        if (optionData.usesJavaExecutor()) {
            return DataExecution.unhandled();
        }
        EventResult result = EventResultResolver.resolve(optionData, random);
        if (result == null) {
            return DataExecution.handled(
                    new EventResult("你没有作出选择。", 0, 0, null));
        }
        return DataExecution.handled(result);
    }

    private List<DataLoader.EventOptionData> visibleOptionData(String eventName,
                                                               Player player) {
        DataLoader.EventData eventData = eventsByName.get(eventName);
        if (eventData == null || eventData.getOptions().isEmpty()) {
            return List.of();
        }
        List<DataLoader.EventOptionData> options = new ArrayList<>();
        for (DataLoader.EventOptionData optionData : eventData.getOptions()) {
            if (optionConditionMatches(optionData, player)) {
                options.add(optionData);
            }
        }
        return options;
    }

    private Map<String, JavaEventExecutor> createJavaExecutors() {
        return Map.of();
    }

    private record DataExecution(boolean handled, EventResult result) {
        static DataExecution handled(EventResult result) {
            return new DataExecution(true, result);
        }

        static DataExecution unhandled() {
            return new DataExecution(false, null);
        }
    }

    @FunctionalInterface
    private interface JavaEventExecutor {
        EventResult execute(int optionIndex, Player player);
    }
}
