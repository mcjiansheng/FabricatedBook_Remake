package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of entering a map node before the node screen resolves.
 */
public class NodeEntryResult {
    private int goldGained;
    private int goldLost;
    private int hpHealed;
    private int hpLost;
    private int damageModifierChange;
    private final List<String> messages = new ArrayList<>();

    public int getGoldGained() { return goldGained; }
    public int getGoldLost() { return goldLost; }
    public int getHpHealed() { return hpHealed; }
    public int getHpLost() { return hpLost; }
    public int getDamageModifierChange() { return damageModifierChange; }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public boolean hasChanges() {
        return goldGained != 0 || goldLost != 0 || hpHealed != 0 || hpLost != 0
                || damageModifierChange != 0 || !messages.isEmpty();
    }

    void gainGold(int amount, String message) {
        if (amount <= 0) return;
        goldGained += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }

    void loseGold(int amount, String message) {
        if (amount <= 0) return;
        goldLost += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }

    void heal(int amount, String message) {
        if (amount <= 0) return;
        hpHealed += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }

    void loseHp(int amount, String message) {
        if (amount <= 0) return;
        hpLost += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }

    void changeDamageModifier(int amount, String message) {
        if (amount == 0) return;
        damageModifierChange += amount;
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }
}
