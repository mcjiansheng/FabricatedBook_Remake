package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.ApplyBuffAction;
import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.action.DoublePoisonAction;
import com.fabricatedbook.core.action.DrawCardAction;
import com.fabricatedbook.core.action.GainBlockAction;
import com.fabricatedbook.core.action.GainEnergyAction;
import com.fabricatedbook.core.action.HealAction;
import com.fabricatedbook.core.action.TriggerWitheringAction;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.buff.ExtraEnergyBuff;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.IntentType;
import com.fabricatedbook.core.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Converts parsed card effect DSL into combat actions or immediate combat-side mutations.
 */
class CardEffectExecutor {
    private final Player player;
    private final List<Enemy> enemies;
    private final Random random;

    CardEffectExecutor(Player player, List<Enemy> enemies, Random random) {
        this.player = player;
        this.enemies = enemies == null ? List.of() : enemies;
        this.random = random == null ? new Random() : random;
    }

    List<CombatAction> execute(Card card, Enemy target, int energySpent) {
        List<CombatAction> actions = new ArrayList<>();
        List<AbstractEntity> aliveEnemies = aliveEnemies();

        for (CardEffect effect : CardEffectParser.parse(card.getEffects())) {
            String[] parts = effect.parts();
            Optional<CardEffectType> effectType = CardEffectType.fromType(effect.getType());
            if (effectType.isEmpty()) {
                System.out.println("[CombatEngine] 未知效果: " + effect.getRaw());
                continue;
            }
            if (!effectType.get().supportsExecution()) {
                System.out.println("[CombatEngine] 未接入执行效果: " + effect.getRaw());
                continue;
            }

            switch (effectType.get()) {
                case DAMAGE: {
                    int dmg = Integer.parseInt(parts[1]);
                    int repeat = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    for (int i = 0; i < repeat; i++) {
                        List<AbstractEntity> singleTarget = singleTarget(target, aliveEnemies);
                        if (!singleTarget.isEmpty()) {
                            actions.add(new DamageAction(player, singleTarget, dmg,
                                    repeat > 1));
                        }
                    }
                    break;
                }
                case DAMAGE_X: {
                    int dmg = Integer.parseInt(parts[1]);
                    for (int i = 0; i < energySpent; i++) {
                        List<AbstractEntity> singleTarget = singleTarget(target, aliveEnemies);
                        if (!singleTarget.isEmpty()) {
                            actions.add(new DamageAction(player, singleTarget, dmg, true));
                        }
                    }
                    break;
                }
                case DAMAGE_ALL: {
                    int dmg = Integer.parseInt(parts[1]);
                    int repeat = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    for (int i = 0; i < repeat; i++) {
                        actions.add(new DamageAction(player,
                                new ArrayList<>(aliveEnemies), dmg, repeat > 1));
                    }
                    break;
                }
                case DAMAGE_ALL_ATTACKING_INTENT: {
                    int dmg = Integer.parseInt(parts[1]);
                    List<AbstractEntity> targets = new ArrayList<>();
                    for (Enemy enemy : enemies) {
                        if (enemy.isAlive() && enemy.getIntent() == IntentType.ATTACK) {
                            targets.add(enemy);
                        }
                    }
                    int repeat = targets.size();
                    for (int i = 0; i < repeat; i++) {
                        actions.add(new DamageAction(player, targets, dmg, repeat > 1));
                    }
                    break;
                }
                case BLOCK: {
                    int block = Integer.parseInt(parts[1]);
                    actions.add(new GainBlockAction(player, block));
                    break;
                }
                case HEAL: {
                    int amount = Integer.parseInt(parts[1]);
                    actions.add(new HealAction(player, amount));
                    break;
                }
                case DRAW: {
                    int count = Integer.parseInt(parts[1]);
                    actions.add(new DrawCardAction(player, count));
                    break;
                }
                case ENERGY: {
                    int amount = Integer.parseInt(parts[1]);
                    actions.add(new GainEnergyAction(player, amount));
                    break;
                }
                case DEBUFF: {
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    List<AbstractEntity> singleTarget = singleTarget(target, aliveEnemies);
                    if (!singleTarget.isEmpty()) {
                        actions.add(new ApplyBuffAction(singleTarget.get(0), buffName, stack));
                    }
                    break;
                }
                case DEBUFF_ALL: {
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    for (AbstractEntity enemy : aliveEnemies) {
                        actions.add(new ApplyBuffAction(enemy, buffName, stack));
                    }
                    break;
                }
                case BUFF: {
                    String buffName = parts[2];
                    int stack = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;
                    if (isExtraEnergyBuff(buffName) && parts.length > 4) {
                        int extraEnergyPerTurn = Integer.parseInt(parts[4]);
                        actions.add(new ApplyBuffAction(player,
                                s -> new ExtraEnergyBuff(s, extraEnergyPerTurn), stack));
                    } else {
                        actions.add(new ApplyBuffAction(player, buffName, stack));
                    }
                    break;
                }
                case PURIFY: {
                    for (BuffHook buff : new ArrayList<>(player.getBuffs())) {
                        String bName = buff.getBuffName();
                        if (bName.equals("Fragile") || bName.equals("BlockReduction")
                                || bName.equals("Weak") || bName.equals("Poison")
                                || bName.equals("Withering") || bName.equals("Dizziness")) {
                            player.removeBuff(bName);
                        }
                    }
                    break;
                }
                case COUNTER: {
                    int blockDamage = player.getBlock();
                    if (blockDamage > 0) {
                        List<AbstractEntity> targets = singleTarget(target, aliveEnemies);
                        if (!targets.isEmpty()) {
                            actions.add(new DamageAction(player, targets, blockDamage));
                        }
                    }
                    break;
                }
                case BONUS_PER_ATTACK: {
                    int bonus = Integer.parseInt(parts[1]);
                    int attackCount = (int) player.getHand().stream()
                            .filter(c -> c.getType() == Card.CardType.ATTACK)
                            .count();
                    addBaseDamageToLastDamageAction(actions, bonus * attackCount);
                    break;
                }
                case BONUS_LOW_HP: {
                    int threshold = Integer.parseInt(parts[1]);
                    int bonus = Integer.parseInt(parts[2]);
                    if (target != null && target.getHp() < threshold && target.isAlive()) {
                        addBaseDamageToLastDamageAction(actions, bonus);
                    }
                    break;
                }
                case DETONATE_WITHERING: {
                    int times = Integer.parseInt(parts[1]);
                    if (target != null && target.isAlive()) {
                        actions.add(new TriggerWitheringAction(target, times));
                    }
                    break;
                }
                case DOUBLE_POISON: {
                    int multiplier = parts.length > 1 ? Integer.parseInt(parts[1]) : 2;
                    actions.add(new DoublePoisonAction(new ArrayList<>(aliveEnemies),
                            multiplier));
                    break;
                }
                case BLOCK_PER_TARGET: {
                    int block = Integer.parseInt(parts[1]);
                    actions.add(new GainBlockAction(player, block * aliveEnemies.size()));
                    break;
                }
                case BONUS_PER_DAMAGE_TAKEN: {
                    int threshold = Integer.parseInt(parts[1]);
                    int bonus = Integer.parseInt(parts[2]);
                    int lostHp = player.getMaxHp() - player.getHp();
                    int extraDmg = (lostHp / threshold) * bonus;
                    if (extraDmg > 0) {
                        addBaseDamageToLastDamageAction(actions, extraDmg);
                    }
                    break;
                }
                case ADD_RANDOM_ATTACK: {
                    List<Card> attackCards = CardPool.getObtainableCardsByProfession(
                                    player.getProfession().name().toLowerCase())
                            .stream().filter(c -> c.getType() == Card.CardType.ATTACK).toList();
                    if (!attackCards.isEmpty()) {
                        Card randomCard = attackCards.get(random.nextInt(attackCards.size()));
                        if (randomCard != null) {
                            player.getHand().add(CardFactory.createFromTemplate(randomCard));
                        }
                    }
                    break;
                }
                case ADD_CARD_TO_DISCARD: {
                    if (parts.length > 1) {
                        Card template = CardPool.findById(parts[1]);
                        if (template != null) {
                            player.getDiscardPile().add(CardFactory.createFromTemplate(template));
                        }
                    }
                    break;
                }
                case STUN_CHANCE: {
                    int chance = Integer.parseInt(parts[1]);
                    if (random.nextInt(100) < chance && target != null && target.isAlive()) {
                        target.setDizzy(true);
                    }
                    break;
                }
                case ESCALATING: {
                    int bonus = Integer.parseInt(parts[1]);
                    card.addEscalatingBonus(bonus);
                    List<AbstractEntity> escTargets = singleTarget(target, aliveEnemies);
                    if (!escTargets.isEmpty() && card.getEscalatingBonus() > 0) {
                        addBaseDamageToLastDamageAction(actions,
                                card.getEscalatingBonus() - bonus);
                    }
                    break;
                }
                case CHANCE_DEBUFF: {
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    int chance = Integer.parseInt(parts[3]);
                    int attempts = Math.max(1, countTrailingDamageActions(actions));
                    for (int i = 0; i < attempts; i++) {
                        if (random.nextInt(100) < chance) {
                            List<AbstractEntity> cdTargets = singleTarget(target,
                                    aliveEnemies);
                            if (!cdTargets.isEmpty()) {
                                actions.add(new ApplyBuffAction(cdTargets.get(0), buffName,
                                        stack));
                            }
                        }
                    }
                    break;
                }
                case POISON_CHANCE: {
                    int chance = Integer.parseInt(parts[1]);
                    int stack = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    int attempts = Math.max(1, countTrailingDamageActions(actions));
                    for (int i = 0; i < attempts; i++) {
                        if (random.nextInt(100) < chance) {
                            List<AbstractEntity> poisonTargets = singleTarget(target,
                                    aliveEnemies);
                            if (!poisonTargets.isEmpty()) {
                                actions.add(new ApplyBuffAction(poisonTargets.get(0),
                                        "Poison", stack));
                            }
                        }
                    }
                    break;
                }
                case TRIGGER_WITHERING: {
                    int times = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (target != null && target.isAlive()) {
                        actions.add(new TriggerWitheringAction(target, times));
                    }
                    break;
                }
                case END_TURN_DAMAGE:
                    break;
            }
        }

        return actions;
    }

    private List<AbstractEntity> aliveEnemies() {
        List<AbstractEntity> alive = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                alive.add(enemy);
            }
        }
        return alive;
    }

    private List<AbstractEntity> singleTarget(Enemy target,
                                              List<AbstractEntity> aliveEnemies) {
        List<AbstractEntity> singleTarget = new ArrayList<>();
        if (target != null && target.isAlive()) {
            singleTarget.add(target);
        } else if (!aliveEnemies.isEmpty()) {
            singleTarget.add(aliveEnemies.get(0));
        }
        return singleTarget;
    }

    private void addBaseDamageToLastDamageAction(List<CombatAction> actions, int amount) {
        if (amount <= 0) return;
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof DamageAction damageAction) {
                actions.set(i, damageAction.withAddedBaseDamage(amount));
                return;
            }
        }
    }

    private int countTrailingDamageActions(List<CombatAction> actions) {
        int count = 0;
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof DamageAction) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private boolean isExtraEnergyBuff(String buffName) {
        return "extra_energy".equalsIgnoreCase(buffName)
                || "extraenergy".equalsIgnoreCase(buffName)
                || "ExtraEnergyBuff".equalsIgnoreCase(buffName);
    }
}
