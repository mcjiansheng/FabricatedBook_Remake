package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.action.ApplyBuffAction;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.action.GainBlockAction;
import com.fabricatedbook.core.action.TriggerWitheringAction;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.event.OnCardUsed;
import com.fabricatedbook.core.event.OnCombatStart;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * DataRelic — 由 JSON 数据驱动的通用藏品实现。
 */
public class DataRelic implements Relic {

    private final RelicData data;
    private final Player owner;
    private final Random random;
    private Consumer<OnCombatStart> combatStartHandler;
    private Consumer<OnCardUsed> cardUsedHandler;

    public DataRelic(RelicData data, Player owner) {
        this(data, owner, new Random());
    }

    DataRelic(RelicData data, Player owner, Random random) {
        this.data = data;
        this.owner = owner;
        this.random = random == null ? new Random() : random;
    }

    public int getEffectValue() {
        return data.getEffectValue();
    }

    public void applyOnPickup() {
        switch (getId()) {
            case "relic_hot_water_flask", "relic_gargoyle_statue" -> {
                owner.addMaxHp(getEffectValue());
                owner.heal(getEffectValue());
            }
            case "relic_old_coin", "relic_flawless_jade", "relic_lucky_coin" ->
                    owner.gainGold(getEffectValue());
            case "relic_rice_bowl" ->
                    owner.heal(Math.max(1, owner.getMaxHp() * getEffectValue() / 100));
            case "relic_speech_draft" -> addRandomCards(1);
            case "relic_prop_box" -> addRandomCards(3);
            case "relic_humility" -> {
                int loss = Math.max(1, owner.getMaxHp() * getEffectValue() / 100);
                owner.setMaxHp(Math.max(1, owner.getMaxHp() - loss));
                owner.setHp(owner.getHp());
            }
            default -> {
                // Most relics are passive and are read by RelicManager.
            }
        }
    }

    public int modifyOutgoingDamage(int damage, AbstractEntity target) {
        double multiplier = 1.0;
        switch (getId()) {
            case "relic_a_combo" -> multiplier += 0.05;
            case "relic_5a_combo" -> multiplier += 0.25;
            case "relic_rosemary_squad" -> multiplier += 0.30;
            case "relic_target" -> {
                if (random.nextInt(100) < 33) multiplier += getEffectValue() / 100.0;
            }
            case "relic_kungfu_manual" -> {
                if (random.nextInt(100) < 33) multiplier += getEffectValue() / 100.0;
            }
            case "relic_avenger" -> {
                if (random.nextInt(100) < 33) multiplier += getEffectValue() / 100.0;
            }
            case "relic_coin_toy" ->
                    multiplier += (owner.getGold() / 10) * getEffectValue() / 100.0;
            case "relic_golden_cup" ->
                    multiplier += (owner.getGold() / 10) * getEffectValue() / 100.0;
            case "relic_old_fan" ->
                    multiplier += deckSize() * getEffectValue() / 100.0;
            case "relic_blade_light" ->
                    multiplier += negativeBuffCount(owner) * getEffectValue() / 100.0;
            case "relic_peeping_eye" -> multiplier += getEffectValue() / 100.0;
            case "relic_king_spear" -> {
                if (owner.getHp() * 10 <= owner.getMaxHp()) {
                    multiplier += getEffectValue() / 100.0;
                }
            }
            case "relic_betrayal" -> {
                if (owner.getCurrentFloor() < 5) multiplier += getEffectValue() / 100.0;
            }
            case "relic_hatred" -> {
                if (owner.getCurrentFloor() < 5) multiplier -= getEffectValue() / 100.0;
            }
            case "relic_frostmourne" ->
                    multiplier += owner.getFrostmourneCombatWins()
                            * getEffectValue() / 100.0;
            case "relic_centralization" ->
                    multiplier += owner.getCentralizationCombatEntries()
                            * getEffectValue() / 100.0;
            default -> {
            }
        }
        return Math.max(0, (int) Math.round(damage * multiplier));
    }

    /**
     * Predictable outgoing damage modifier used by UI previews.
     * Probabilistic relics are intentionally excluded so previews never roll
     * random outcomes ahead of card play.
     */
    public int previewOutgoingDamage(int damage, AbstractEntity target) {
        double multiplier = 1.0;
        switch (getId()) {
            case "relic_a_combo" -> multiplier += 0.05;
            case "relic_5a_combo" -> multiplier += 0.25;
            case "relic_rosemary_squad" -> multiplier += 0.30;
            case "relic_coin_toy" ->
                    multiplier += (owner.getGold() / 10) * getEffectValue() / 100.0;
            case "relic_golden_cup" ->
                    multiplier += (owner.getGold() / 10) * getEffectValue() / 100.0;
            case "relic_old_fan" ->
                    multiplier += deckSize() * getEffectValue() / 100.0;
            case "relic_blade_light" ->
                    multiplier += negativeBuffCount(owner) * getEffectValue() / 100.0;
            case "relic_peeping_eye" -> multiplier += getEffectValue() / 100.0;
            case "relic_king_spear" -> {
                if (owner.getHp() * 10 <= owner.getMaxHp()) {
                    multiplier += getEffectValue() / 100.0;
                }
            }
            case "relic_betrayal" -> {
                if (owner.getCurrentFloor() < 5) multiplier += getEffectValue() / 100.0;
            }
            case "relic_hatred" -> {
                if (owner.getCurrentFloor() < 5) multiplier -= getEffectValue() / 100.0;
            }
            case "relic_frostmourne" ->
                    multiplier += owner.getFrostmourneCombatWins()
                            * getEffectValue() / 100.0;
            case "relic_centralization" ->
                    multiplier += owner.getCentralizationCombatEntries()
                            * getEffectValue() / 100.0;
            default -> {
            }
        }
        return Math.max(0, (int) Math.round(damage * multiplier));
    }

    public int modifyIncomingDamage(int damage) {
        double multiplier = 1.0;
        switch (getId()) {
            case "relic_rosemary_squad" -> multiplier -= 0.30;
            case "relic_stop_war" -> multiplier *= getEffectValue() / 100.0;
            case "relic_tolerance" -> multiplier += getEffectValue() / 100.0;
            case "relic_safety_suit" -> multiplier -= cursedRelicCount() * getEffectValue() / 100.0;
            default -> {
            }
        }
        return Math.max(0, (int) Math.round(damage * Math.max(0.1, multiplier)));
    }

    public int previewIncomingDamage(int damage) {
        return modifyIncomingDamage(damage);
    }

    public int modifyGoldReward(int gold) {
        if ("relic_treasure_ring".equals(getId())) {
            return gold + gold * getEffectValue() / 100;
        }
        if ("relic_king_crystal".equals(getId()) && owner.getHp() * 10 > owner.getMaxHp()) {
            owner.takeDamage(5);
            return gold + getEffectValue();
        }
        return gold;
    }

    public int modifyHeal(int heal) {
        if ("relic_hand_soap".equals(getId()) || "relic_pale_crown".equals(getId())) {
            return heal + heal * getEffectValue() / 100;
        }
        return heal;
    }

    public int modifyStatusDamage(int damage, AbstractEntity target, String statusType) {
        double multiplier = 1.0;
        if (target instanceof Enemy) {
            if ("Poison".equals(statusType) && "relic_marionette".equals(getId())) {
                multiplier += getEffectValue() / 100.0;
            }
            if ("Withering".equals(statusType)) {
                if ("relic_marionette".equals(getId())) {
                    multiplier += getEffectValue() / 100.0;
                }
                if ("relic_wither_storm".equals(getId())) {
                    multiplier += getEffectValue() / 100.0;
                }
            }
        }
        return Math.max(0, (int) Math.round(damage * multiplier));
    }

    public void onTurnStart(List<Enemy> enemies) {
        if ("relic_king_armor".equals(getId())
                && owner.getHp() * 10 <= owner.getMaxHp()) {
            new ApplyBuffAction(owner, "BlockIncrease", 1).execute();
            new GainBlockAction(owner, getEffectValue()).execute();
        } else if ("relic_i_help_you".equals(getId()) && enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    new DamageAction(owner, List.of(enemy), getEffectValue()).execute();
                }
            }
        } else if ("relic_wither_storm".equals(getId()) && enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive() && hasBuff(enemy, "Withering")) {
                    new TriggerWitheringAction(enemy, 1).execute();
                }
            }
        }
    }

    public void onCombatVictory() {
        if ("relic_frostmourne".equals(getId())) {
            owner.incrementFrostmourneCombatWins();
        }
    }

    public int modifyRelicRewardChance(int chancePercent) {
        if ("relic_vampire_count".equals(getId())) {
            return chancePercent + 15;
        }
        return chancePercent;
    }

    public void modifyEnemyAtCombatStart(List<Enemy> enemies) {
        if (enemies == null) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (enemy == null) continue;
            if ("relic_sea_metabolism".equals(getId())) {
                int increase = Math.max(1, enemy.getMaxHp() * getEffectValue() / 100);
                enemy.setMaxHp(enemy.getMaxHp() + increase);
                enemy.setHp(enemy.getHp() + increase);
            } else if ("relic_betrayal".equals(getId()) && owner.getCurrentFloor() >= 5) {
                int increase = Math.max(1, enemy.getMaxHp() * getEffectValue() / 100);
                enemy.setMaxHp(enemy.getMaxHp() + increase);
                enemy.setHp(enemy.getHp() + increase);
            } else if ("relic_hatred".equals(getId()) && owner.getCurrentFloor() >= 5) {
                int decrease = Math.max(1, enemy.getMaxHp() * getEffectValue() / 100);
                int newMaxHp = Math.max(1, enemy.getMaxHp() - decrease);
                enemy.setMaxHp(newMaxHp);
                enemy.setHp(Math.min(enemy.getHp(), newMaxHp));
            }
        }
    }

    @Override
    public void subscribe(EventBus bus) {
        if ("relic_weird_flute".equals(getId())) {
            combatStartHandler = event -> owner.gainEnergy(getEffectValue());
            bus.subscribe(OnCombatStart.class, combatStartHandler);
        } else if ("relic_miniature_stage".equals(getId())) {
            combatStartHandler = event -> {
                owner.gainEnergy(getEffectValue());
                owner.drawCards(2);
            };
            bus.subscribe(OnCombatStart.class, combatStartHandler);
        }

        if ("relic_vampire_fang".equals(getId())) {
            cardUsedHandler = event -> owner.heal(getEffectValue());
            bus.subscribe(OnCardUsed.class, cardUsedHandler);
        }
    }

    @Override
    public void unsubscribe(EventBus bus) {
        if (combatStartHandler != null) {
            bus.unsubscribe(OnCombatStart.class, combatStartHandler);
            combatStartHandler = null;
        }
        if (cardUsedHandler != null) {
            bus.unsubscribe(OnCardUsed.class, cardUsedHandler);
            cardUsedHandler = null;
        }
    }

    private void addRandomCards(int count) {
        List<Card> pool = CardPool.getObtainableCardsByProfession(
                owner.getProfession().name().toLowerCase());
        if (pool.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            Card template = pool.get(random.nextInt(pool.size()));
            owner.getDrawPile().add(CardFactory.createFromTemplate(template));
            owner.setCardCount(owner.getCardCount() + 1);
        }
    }

    private int deckSize() {
        return owner.getDrawPile().size() + owner.getHand().size()
                + owner.getDiscardPile().size() + owner.getExhaustPile().size();
    }

    private int cursedRelicCount() {
        int count = 0;
        for (Relic relic : owner.getRelics()) {
            if (relic.getRarity() == Rarity.CURSED) count++;
        }
        return count;
    }

    private int negativeBuffCount(AbstractEntity entity) {
        int count = 0;
        for (BuffHook buff : entity.getBuffs()) {
            if (isNegativeBuff(buff.getBuffName()) && buff.getStack() > 0) {
                count++;
            }
        }
        return count;
    }

    private boolean hasBuff(AbstractEntity entity, String buffName) {
        for (BuffHook buff : entity.getBuffs()) {
            if (buff.getBuffName().equals(buffName) && buff.getStack() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isNegativeBuff(String buffName) {
        return "Fragile".equals(buffName) || "BlockReduction".equals(buffName)
                || "Weak".equals(buffName) || "Poison".equals(buffName)
                || "Withering".equals(buffName) || "Dizziness".equals(buffName);
    }

    @Override public String getName() { return data.getName(); }
    @Override public String getDescription() { return data.getDescription(); }
    @Override public Rarity getRarity() { return data.getRarity(); }
    @Override public String getId() { return data.getId(); }
}
