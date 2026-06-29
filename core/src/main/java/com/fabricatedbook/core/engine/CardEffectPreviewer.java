package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.IntentType;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies parsed card effect DSL to deterministic combat preview totals.
 */
class CardEffectPreviewer {
    private final Card card;
    private final Player player;
    private final List<Enemy> aliveEnemies;
    private final Enemy selectedTarget;
    private final AbstractEntity damageTarget;
    private final AbstractEntity previewPlayer;
    private final RelicManager relicManager;
    private final int environmentDamageModifier;
    private final Map<CardEffectType, PreviewHandler> handlers;

    CardEffectPreviewer(Card card, Player player, List<Enemy> aliveEnemies,
                        Enemy selectedTarget, AbstractEntity damageTarget,
                        AbstractEntity previewPlayer, RelicManager relicManager,
                        int environmentDamageModifier) {
        this.card = card;
        this.player = player;
        this.aliveEnemies = aliveEnemies;
        this.selectedTarget = selectedTarget;
        this.damageTarget = damageTarget;
        this.previewPlayer = previewPlayer;
        this.relicManager = relicManager;
        this.environmentDamageModifier = environmentDamageModifier;
        this.handlers = createHandlers();
    }

    void apply(List<CardEffect> effects, CombatPreviewCalculator.PreviewTotals totals) {
        for (CardEffect effect : effects) {
            apply(effect, totals);
        }
    }

    private void apply(CardEffect effect, CombatPreviewCalculator.PreviewTotals totals) {
        String[] parts = effect.parts();
        Optional<CardEffectType> effectType = CardEffectType.fromType(effect.getType());
        if (effectType.isEmpty() || !effectType.get().supportsPreview()) {
            return;
        }

        PreviewHandler handler = handlers.get(effectType.get());
        if (handler != null) {
            handler.preview(new PreviewContext(parts, totals));
        }
    }

    private Map<CardEffectType, PreviewHandler> createHandlers() {
        Map<CardEffectType, PreviewHandler> registered = new EnumMap<>(CardEffectType.class);
        registered.put(CardEffectType.DAMAGE, this::previewDamage);
        registered.put(CardEffectType.DAMAGE_X, this::previewDamageX);
        registered.put(CardEffectType.DAMAGE_ALL, this::previewDamageAll);
        registered.put(CardEffectType.DAMAGE_ALL_ATTACKING_INTENT,
                this::previewDamageAllAttackingIntent);
        registered.put(CardEffectType.COUNTER, this::previewCounter);
        registered.put(CardEffectType.BONUS_PER_ATTACK, this::previewBonusPerAttack);
        registered.put(CardEffectType.BONUS_LOW_HP, this::previewBonusLowHp);
        registered.put(CardEffectType.BONUS_PER_DAMAGE_TAKEN,
                this::previewBonusPerDamageTaken);
        registered.put(CardEffectType.ESCALATING, this::previewEscalating);
        registered.put(CardEffectType.BLOCK, this::previewBlock);
        registered.put(CardEffectType.BLOCK_PER_TARGET, this::previewBlockPerTarget);
        registered.put(CardEffectType.BUFF, this::previewBuff);
        registered.put(CardEffectType.DEBUFF, this::previewDebuff);
        registered.put(CardEffectType.DEBUFF_ALL, this::previewDebuffAll);
        return Map.copyOf(registered);
    }

    private void previewDamage(PreviewContext context) {
        if (damageTarget == null || context.parts().length < 2) {
            return;
        }
        int baseDamage = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        int repeat = context.parts().length > 2
                ? CombatPreviewCalculator.parseInt(context.parts()[2], 1)
                : 1;
        addDamage(context.totals(), baseDamage, repeat, damageTarget);
    }

    private void previewDamageX(PreviewContext context) {
        if (damageTarget == null || context.parts().length < 2) {
            return;
        }
        int baseDamage = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        int repeat = Math.max(0, player.getEnergy());
        if (repeat > 0) {
            addDamage(context.totals(), baseDamage, repeat, damageTarget);
        }
    }

    private void previewDamageAll(PreviewContext context) {
        if (aliveEnemies.isEmpty() || context.parts().length < 2) {
            return;
        }
        int baseDamage = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        int repeat = context.parts().length > 2
                ? CombatPreviewCalculator.parseInt(context.parts()[2], 1)
                : 1;
        addDamage(context.totals(), baseDamage, repeat,
                CombatPreviewCalculator.commonEnemyTarget(aliveEnemies));
    }

    private void previewDamageAllAttackingIntent(PreviewContext context) {
        if (aliveEnemies.isEmpty() || context.parts().length < 2) {
            return;
        }
        List<Enemy> attackingEnemies = aliveEnemies.stream()
                .filter(enemy -> enemy.getIntent() == IntentType.ATTACK)
                .toList();
        int attackingCount = attackingEnemies.size();
        if (attackingCount <= 0) {
            return;
        }
        int baseDamage = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        addDamage(context.totals(), baseDamage, attackingCount,
                CombatPreviewCalculator.commonEnemyTarget(attackingEnemies));
    }

    private void previewCounter(PreviewContext context) {
        if (damageTarget == null || context.parts().length < 2) {
            return;
        }
        if ("block".equalsIgnoreCase(context.parts()[1]) && player.getBlock() > 0) {
            addDamage(context.totals(), player.getBlock(), 1, damageTarget);
        }
    }

    private void previewBonusPerAttack(PreviewContext context) {
        if (damageTarget == null || context.parts().length < 2
                || !context.totals().hasDamage()) {
            return;
        }
        int bonus = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        int attackCount = (int) player.getHand().stream()
                .filter(c -> c.getType() == Card.CardType.ATTACK)
                .count();
        CombatPreviewCalculator.addBaseDamageToLastPreview(context.totals(),
                bonus * attackCount);
    }

    private void previewBonusLowHp(PreviewContext context) {
        if (selectedTarget == null || !selectedTarget.isAlive()
                || context.parts().length < 3) {
            return;
        }
        int threshold = CombatPreviewCalculator.parseInt(context.parts()[1], 0);
        int bonus = CombatPreviewCalculator.parseInt(context.parts()[2], 0);
        if (selectedTarget.getHp() < threshold) {
            CombatPreviewCalculator.addBaseDamageToLastPreview(context.totals(), bonus);
        }
    }

    private void previewBonusPerDamageTaken(PreviewContext context) {
        if (damageTarget == null || context.parts().length < 3) {
            return;
        }
        int threshold = CombatPreviewCalculator.parseInt(context.parts()[1], 1);
        int bonus = CombatPreviewCalculator.parseInt(context.parts()[2], 0);
        int lostHp = player.getMaxHp() - player.getHp();
        int extraDamage = (lostHp / Math.max(1, threshold)) * bonus;
        if (extraDamage > 0) {
            CombatPreviewCalculator.addBaseDamageToLastPreview(context.totals(),
                    extraDamage);
        }
    }

    private void previewEscalating(PreviewContext context) {
        if (damageTarget == null || card.getEscalatingBonus() <= 0) {
            return;
        }
        int bonus = CombatPreviewCalculator.parseInt(
                context.parts().length > 1 ? context.parts()[1] : "0", 0);
        int alreadyStoredBonus = Math.max(0, card.getEscalatingBonus() - bonus);
        if (alreadyStoredBonus > 0) {
            CombatPreviewCalculator.addBaseDamageToLastPreview(context.totals(),
                    alreadyStoredBonus);
        }
    }

    private void previewBlock(PreviewContext context) {
        if (context.parts().length < 2) {
            return;
        }
        context.totals().addBlock(DamageCalculator.calculateBlock(
                CombatPreviewCalculator.parseInt(context.parts()[1], 0),
                previewPlayer));
    }

    private void previewBlockPerTarget(PreviewContext context) {
        if (context.parts().length < 2) {
            return;
        }
        int baseBlock = CombatPreviewCalculator.parseInt(context.parts()[1], 0)
                * aliveEnemies.size();
        context.totals().addBlock(DamageCalculator.calculateBlock(baseBlock,
                previewPlayer));
    }

    private void previewBuff(PreviewContext context) {
        if (context.parts().length >= 3 && "self".equalsIgnoreCase(context.parts()[1])) {
            int stack = context.parts().length > 3
                    ? CombatPreviewCalculator.parseInt(context.parts()[3], 1)
                    : 1;
            CombatPreviewCalculator.applyBuff(previewPlayer, context.parts()[2], stack);
        }
    }

    private void previewDebuff(PreviewContext context) {
        if (damageTarget != null && context.parts().length >= 3) {
            CombatPreviewCalculator.applyBuff(damageTarget, context.parts()[1],
                    CombatPreviewCalculator.parseInt(context.parts()[2], 1));
        }
    }

    private void previewDebuffAll(PreviewContext context) {
        if (damageTarget != null && context.parts().length >= 3) {
            CombatPreviewCalculator.applyBuff(damageTarget, context.parts()[1],
                    CombatPreviewCalculator.parseInt(context.parts()[2], 1));
        }
    }

    private void addDamage(CombatPreviewCalculator.PreviewTotals totals, int baseDamage,
                           int repeat, AbstractEntity target) {
        CombatPreviewCalculator.addDamage(totals, baseDamage, repeat, previewPlayer,
                player, target, relicManager, environmentDamageModifier);
    }

    private interface PreviewHandler {
        void preview(PreviewContext context);
    }

    private record PreviewContext(
            String[] parts,
            CombatPreviewCalculator.PreviewTotals totals
    ) {}
}
