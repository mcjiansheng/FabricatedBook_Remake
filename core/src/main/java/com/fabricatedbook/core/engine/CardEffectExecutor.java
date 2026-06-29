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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Converts parsed card effect DSL into combat actions or immediate combat-side mutations.
 */
class CardEffectExecutor {
    private final Player player;
    private final List<Enemy> enemies;
    private final Random random;
    private final Map<CardEffectType, EffectHandler> handlers;

    CardEffectExecutor(Player player, List<Enemy> enemies, Random random) {
        this.player = player;
        this.enemies = enemies == null ? List.of() : enemies;
        this.random = random == null ? new Random() : random;
        this.handlers = createHandlers();
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

            EffectHandler handler = handlers.get(effectType.get());
            if (handler == null) {
                System.out.println("[CombatEngine] 未接入执行 handler: " + effect.getRaw());
                continue;
            }
            handler.execute(new ExecutionContext(card, target, energySpent, parts,
                    aliveEnemies, actions));
        }

        return actions;
    }

    private Map<CardEffectType, EffectHandler> createHandlers() {
        Map<CardEffectType, EffectHandler> registered = new EnumMap<>(CardEffectType.class);
        registered.put(CardEffectType.DAMAGE, this::applyDamage);
        registered.put(CardEffectType.DAMAGE_X, this::applyDamageX);
        registered.put(CardEffectType.DAMAGE_ALL, this::applyDamageAll);
        registered.put(CardEffectType.DAMAGE_ALL_ATTACKING_INTENT,
                this::applyDamageAllAttackingIntent);
        registered.put(CardEffectType.BLOCK, this::applyBlock);
        registered.put(CardEffectType.HEAL, this::applyHeal);
        registered.put(CardEffectType.DRAW, this::applyDraw);
        registered.put(CardEffectType.ENERGY, this::applyEnergy);
        registered.put(CardEffectType.DEBUFF, this::applyDebuff);
        registered.put(CardEffectType.DEBUFF_ALL, this::applyDebuffAll);
        registered.put(CardEffectType.BUFF, this::applyBuff);
        registered.put(CardEffectType.PURIFY, this::applyPurify);
        registered.put(CardEffectType.COUNTER, this::applyCounter);
        registered.put(CardEffectType.BONUS_PER_ATTACK, this::applyBonusPerAttack);
        registered.put(CardEffectType.BONUS_LOW_HP, this::applyBonusLowHp);
        registered.put(CardEffectType.DETONATE_WITHERING, this::applyDetonateWithering);
        registered.put(CardEffectType.DOUBLE_POISON, this::applyDoublePoison);
        registered.put(CardEffectType.BLOCK_PER_TARGET, this::applyBlockPerTarget);
        registered.put(CardEffectType.BONUS_PER_DAMAGE_TAKEN,
                this::applyBonusPerDamageTaken);
        registered.put(CardEffectType.ADD_RANDOM_ATTACK, this::applyAddRandomAttack);
        registered.put(CardEffectType.ADD_CARD_TO_DISCARD, this::applyAddCardToDiscard);
        registered.put(CardEffectType.STUN_CHANCE, this::applyStunChance);
        registered.put(CardEffectType.ESCALATING, this::applyEscalating);
        registered.put(CardEffectType.CHANCE_DEBUFF, this::applyChanceDebuff);
        registered.put(CardEffectType.POISON_CHANCE, this::applyPoisonChance);
        registered.put(CardEffectType.TRIGGER_WITHERING, this::applyTriggerWithering);
        registered.put(CardEffectType.END_TURN_DAMAGE, context -> {});
        return Map.copyOf(registered);
    }

    private void applyDamage(ExecutionContext context) {
        int dmg = Integer.parseInt(context.parts()[1]);
        int repeat = context.parts().length > 2 ? Integer.parseInt(context.parts()[2]) : 1;
        for (int i = 0; i < repeat; i++) {
            List<AbstractEntity> singleTarget = singleTarget(context.target(),
                    context.aliveEnemies());
            if (!singleTarget.isEmpty()) {
                context.actions().add(new DamageAction(player, singleTarget, dmg, repeat > 1));
            }
        }
    }

    private void applyDamageX(ExecutionContext context) {
        int dmg = Integer.parseInt(context.parts()[1]);
        for (int i = 0; i < context.energySpent(); i++) {
            List<AbstractEntity> singleTarget = singleTarget(context.target(),
                    context.aliveEnemies());
            if (!singleTarget.isEmpty()) {
                context.actions().add(new DamageAction(player, singleTarget, dmg, true));
            }
        }
    }

    private void applyDamageAll(ExecutionContext context) {
        int dmg = Integer.parseInt(context.parts()[1]);
        int repeat = context.parts().length > 2 ? Integer.parseInt(context.parts()[2]) : 1;
        for (int i = 0; i < repeat; i++) {
            context.actions().add(new DamageAction(player,
                    new ArrayList<>(context.aliveEnemies()), dmg, repeat > 1));
        }
    }

    private void applyDamageAllAttackingIntent(ExecutionContext context) {
        int dmg = Integer.parseInt(context.parts()[1]);
        List<AbstractEntity> targets = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getIntent() == IntentType.ATTACK) {
                targets.add(enemy);
            }
        }
        int repeat = targets.size();
        for (int i = 0; i < repeat; i++) {
            context.actions().add(new DamageAction(player, targets, dmg, repeat > 1));
        }
    }

    private void applyBlock(ExecutionContext context) {
        int block = Integer.parseInt(context.parts()[1]);
        context.actions().add(new GainBlockAction(player, block));
    }

    private void applyHeal(ExecutionContext context) {
        int amount = Integer.parseInt(context.parts()[1]);
        context.actions().add(new HealAction(player, amount));
    }

    private void applyDraw(ExecutionContext context) {
        int count = Integer.parseInt(context.parts()[1]);
        context.actions().add(new DrawCardAction(player, count));
    }

    private void applyEnergy(ExecutionContext context) {
        int amount = Integer.parseInt(context.parts()[1]);
        context.actions().add(new GainEnergyAction(player, amount));
    }

    private void applyDebuff(ExecutionContext context) {
        String buffName = context.parts()[1];
        int stack = Integer.parseInt(context.parts()[2]);
        List<AbstractEntity> singleTarget = singleTarget(context.target(),
                context.aliveEnemies());
        if (!singleTarget.isEmpty()) {
            context.actions().add(new ApplyBuffAction(singleTarget.get(0), buffName, stack));
        }
    }

    private void applyDebuffAll(ExecutionContext context) {
        String buffName = context.parts()[1];
        int stack = Integer.parseInt(context.parts()[2]);
        for (AbstractEntity enemy : context.aliveEnemies()) {
            context.actions().add(new ApplyBuffAction(enemy, buffName, stack));
        }
    }

    private void applyBuff(ExecutionContext context) {
        String buffName = context.parts()[2];
        int stack = context.parts().length > 3 ? Integer.parseInt(context.parts()[3]) : 1;
        if (isExtraEnergyBuff(buffName) && context.parts().length > 4) {
            int extraEnergyPerTurn = Integer.parseInt(context.parts()[4]);
            context.actions().add(new ApplyBuffAction(player,
                    s -> new ExtraEnergyBuff(s, extraEnergyPerTurn), stack));
        } else {
            context.actions().add(new ApplyBuffAction(player, buffName, stack));
        }
    }

    private void applyPurify(ExecutionContext context) {
        for (BuffHook buff : new ArrayList<>(player.getBuffs())) {
            String bName = buff.getBuffName();
            if (bName.equals("Fragile") || bName.equals("BlockReduction")
                    || bName.equals("Weak") || bName.equals("Poison")
                    || bName.equals("Withering") || bName.equals("Dizziness")) {
                player.removeBuff(bName);
            }
        }
    }

    private void applyCounter(ExecutionContext context) {
        int blockDamage = player.getBlock();
        if (blockDamage > 0) {
            List<AbstractEntity> targets = singleTarget(context.target(),
                    context.aliveEnemies());
            if (!targets.isEmpty()) {
                context.actions().add(new DamageAction(player, targets, blockDamage));
            }
        }
    }

    private void applyBonusPerAttack(ExecutionContext context) {
        int bonus = Integer.parseInt(context.parts()[1]);
        int attackCount = (int) player.getHand().stream()
                .filter(c -> c.getType() == Card.CardType.ATTACK)
                .count();
        addBaseDamageToLastDamageAction(context.actions(), bonus * attackCount);
    }

    private void applyBonusLowHp(ExecutionContext context) {
        int threshold = Integer.parseInt(context.parts()[1]);
        int bonus = Integer.parseInt(context.parts()[2]);
        if (context.target() != null && context.target().getHp() < threshold
                && context.target().isAlive()) {
            addBaseDamageToLastDamageAction(context.actions(), bonus);
        }
    }

    private void applyDetonateWithering(ExecutionContext context) {
        int times = Integer.parseInt(context.parts()[1]);
        if (context.target() != null && context.target().isAlive()) {
            context.actions().add(new TriggerWitheringAction(context.target(), times));
        }
    }

    private void applyDoublePoison(ExecutionContext context) {
        int multiplier = context.parts().length > 1 ? Integer.parseInt(context.parts()[1]) : 2;
        context.actions().add(new DoublePoisonAction(
                new ArrayList<>(context.aliveEnemies()), multiplier));
    }

    private void applyBlockPerTarget(ExecutionContext context) {
        int block = Integer.parseInt(context.parts()[1]);
        context.actions().add(new GainBlockAction(player,
                block * context.aliveEnemies().size()));
    }

    private void applyBonusPerDamageTaken(ExecutionContext context) {
        int threshold = Integer.parseInt(context.parts()[1]);
        int bonus = Integer.parseInt(context.parts()[2]);
        int lostHp = player.getMaxHp() - player.getHp();
        int extraDmg = (lostHp / threshold) * bonus;
        if (extraDmg > 0) {
            addBaseDamageToLastDamageAction(context.actions(), extraDmg);
        }
    }

    private void applyAddRandomAttack(ExecutionContext context) {
        List<Card> attackCards = CardPool.getObtainableCardsByProfession(
                        player.getProfession().name().toLowerCase())
                .stream().filter(c -> c.getType() == Card.CardType.ATTACK).toList();
        if (!attackCards.isEmpty()) {
            Card randomCard = attackCards.get(random.nextInt(attackCards.size()));
            if (randomCard != null) {
                player.getHand().add(CardFactory.createFromTemplate(randomCard));
            }
        }
    }

    private void applyAddCardToDiscard(ExecutionContext context) {
        if (context.parts().length > 1) {
            Card template = CardPool.findById(context.parts()[1]);
            if (template != null) {
                player.getDiscardPile().add(CardFactory.createFromTemplate(template));
            }
        }
    }

    private void applyStunChance(ExecutionContext context) {
        int chance = Integer.parseInt(context.parts()[1]);
        if (random.nextInt(100) < chance && context.target() != null
                && context.target().isAlive()) {
            context.target().setDizzy(true);
        }
    }

    private void applyEscalating(ExecutionContext context) {
        int bonus = Integer.parseInt(context.parts()[1]);
        context.card().addEscalatingBonus(bonus);
        List<AbstractEntity> escTargets = singleTarget(context.target(),
                context.aliveEnemies());
        if (!escTargets.isEmpty() && context.card().getEscalatingBonus() > 0) {
            addBaseDamageToLastDamageAction(context.actions(),
                    context.card().getEscalatingBonus() - bonus);
        }
    }

    private void applyChanceDebuff(ExecutionContext context) {
        String buffName = context.parts()[1];
        int stack = Integer.parseInt(context.parts()[2]);
        int chance = Integer.parseInt(context.parts()[3]);
        int attempts = Math.max(1, countTrailingDamageActions(context.actions()));
        for (int i = 0; i < attempts; i++) {
            if (random.nextInt(100) < chance) {
                List<AbstractEntity> cdTargets = singleTarget(context.target(),
                        context.aliveEnemies());
                if (!cdTargets.isEmpty()) {
                    context.actions().add(new ApplyBuffAction(cdTargets.get(0), buffName,
                            stack));
                }
            }
        }
    }

    private void applyPoisonChance(ExecutionContext context) {
        int chance = Integer.parseInt(context.parts()[1]);
        int stack = context.parts().length > 2 ? Integer.parseInt(context.parts()[2]) : 1;
        int attempts = Math.max(1, countTrailingDamageActions(context.actions()));
        for (int i = 0; i < attempts; i++) {
            if (random.nextInt(100) < chance) {
                List<AbstractEntity> poisonTargets = singleTarget(context.target(),
                        context.aliveEnemies());
                if (!poisonTargets.isEmpty()) {
                    context.actions().add(new ApplyBuffAction(poisonTargets.get(0),
                            "Poison", stack));
                }
            }
        }
    }

    private void applyTriggerWithering(ExecutionContext context) {
        int times = context.parts().length > 1 ? Integer.parseInt(context.parts()[1]) : 1;
        if (context.target() != null && context.target().isAlive()) {
            context.actions().add(new TriggerWitheringAction(context.target(), times));
        }
    }

    private interface EffectHandler {
        void execute(ExecutionContext context);
    }

    private record ExecutionContext(
            Card card,
            Enemy target,
            int energySpent,
            String[] parts,
            List<AbstractEntity> aliveEnemies,
            List<CombatAction> actions
    ) {}

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
