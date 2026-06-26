package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of entering a map node before the node screen resolves.
 */
public class NodeEntryResult {
    private int goldGained;
    private final List<String> messages = new ArrayList<>();

    public int getGoldGained() { return goldGained; }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public boolean hasChanges() {
        return goldGained != 0 || !messages.isEmpty();
    }

    void gainGold(int amount, String message) {
        if (amount <= 0) return;
        goldGained += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }
}
