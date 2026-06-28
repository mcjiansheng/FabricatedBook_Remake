package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.IntentType;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.List;

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
    }

    void apply(List<CardEffect> effects, CombatPreviewCalculator.PreviewTotals totals) {
        for (CardEffect effect : effects) {
            apply(effect, totals);
        }
    }

    private void apply(CardEffect effect, CombatPreviewCalculator.PreviewTotals totals) {
        String[] parts = effect.parts();
        String type = effect.getType();

        switch (type) {
            case "damage" -> {
                if (damageTarget == null || parts.length < 2) break;
                int baseDamage = CombatPreviewCalculator.parseInt(parts[1], 0);
                int repeat = parts.length > 2
                        ? CombatPreviewCalculator.parseInt(parts[2], 1)
                        : 1;
                addDamage(totals, baseDamage, repeat, damageTarget);
            }
            case "damage_x" -> {
                if (damageTarget == null || parts.length < 2) break;
                int baseDamage = CombatPreviewCalculator.parseInt(parts[1], 0);
                int repeat = Math.max(0, player.getEnergy());
                if (repeat > 0) {
                    addDamage(totals, baseDamage, repeat, damageTarget);
                }
            }
            case "damage_all" -> {
                if (aliveEnemies.isEmpty() || parts.length < 2) break;
                int baseDamage = CombatPreviewCalculator.parseInt(parts[1], 0);
                int repeat = parts.length > 2
                        ? CombatPreviewCalculator.parseInt(parts[2], 1)
                        : 1;
                addDamage(totals, baseDamage, repeat,
                        CombatPreviewCalculator.commonEnemyTarget(aliveEnemies));
            }
            case "damage_all_attacking_intent" -> {
                if (aliveEnemies.isEmpty() || parts.length < 2) break;
                List<Enemy> attackingEnemies = aliveEnemies.stream()
                        .filter(enemy -> enemy.getIntent() == IntentType.ATTACK)
                        .toList();
                int attackingCount = attackingEnemies.size();
                if (attackingCount <= 0) break;
                int baseDamage = CombatPreviewCalculator.parseInt(parts[1], 0);
                addDamage(totals, baseDamage, attackingCount,
                        CombatPreviewCalculator.commonEnemyTarget(attackingEnemies));
            }
            case "counter" -> {
                if (damageTarget == null || parts.length < 2) break;
                if ("block".equalsIgnoreCase(parts[1]) && player.getBlock() > 0) {
                    addDamage(totals, player.getBlock(), 1, damageTarget);
                }
            }
            case "bonus_per_attack" -> {
                if (damageTarget == null || parts.length < 2 || !totals.hasDamage()) {
                    break;
                }
                int bonus = CombatPreviewCalculator.parseInt(parts[1], 0);
                int attackCount = (int) player.getHand().stream()
                        .filter(c -> c.getType() == Card.CardType.ATTACK)
                        .count();
                CombatPreviewCalculator.addBaseDamageToLastPreview(totals,
                        bonus * attackCount);
            }
            case "bonus_low_hp" -> {
                if (selectedTarget == null || !selectedTarget.isAlive()
                        || parts.length < 3) {
                    break;
                }
                int threshold = CombatPreviewCalculator.parseInt(parts[1], 0);
                int bonus = CombatPreviewCalculator.parseInt(parts[2], 0);
                if (selectedTarget.getHp() < threshold) {
                    CombatPreviewCalculator.addBaseDamageToLastPreview(totals, bonus);
                }
            }
            case "bonus_per_damage_taken" -> {
                if (damageTarget == null || parts.length < 3) break;
                int threshold = CombatPreviewCalculator.parseInt(parts[1], 1);
                int bonus = CombatPreviewCalculator.parseInt(parts[2], 0);
                int lostHp = player.getMaxHp() - player.getHp();
                int extraDamage = (lostHp / Math.max(1, threshold)) * bonus;
                if (extraDamage > 0) {
                    CombatPreviewCalculator.addBaseDamageToLastPreview(totals, extraDamage);
                }
            }
            case "escalating" -> {
                if (damageTarget == null || card.getEscalatingBonus() <= 0) break;
                int bonus = CombatPreviewCalculator.parseInt(
                        parts.length > 1 ? parts[1] : "0", 0);
                int alreadyStoredBonus = Math.max(0, card.getEscalatingBonus() - bonus);
                if (alreadyStoredBonus > 0) {
                    CombatPreviewCalculator.addBaseDamageToLastPreview(totals,
                            alreadyStoredBonus);
                }
            }
            case "block" -> {
                if (parts.length < 2) break;
                totals.addBlock(DamageCalculator.calculateBlock(
                        CombatPreviewCalculator.parseInt(parts[1], 0), previewPlayer));
            }
            case "block_per_target" -> {
                if (parts.length < 2) break;
                int baseBlock = CombatPreviewCalculator.parseInt(parts[1], 0)
                        * aliveEnemies.size();
                totals.addBlock(DamageCalculator.calculateBlock(baseBlock, previewPlayer));
            }
            case "buff" -> {
                if (parts.length >= 3 && "self".equalsIgnoreCase(parts[1])) {
                    int stack = parts.length > 3
                            ? CombatPreviewCalculator.parseInt(parts[3], 1)
                            : 1;
                    CombatPreviewCalculator.applyBuff(previewPlayer, parts[2], stack);
                }
            }
            case "debuff" -> {
                if (damageTarget != null && parts.length >= 3) {
                    CombatPreviewCalculator.applyBuff(damageTarget, parts[1],
                            CombatPreviewCalculator.parseInt(parts[2], 1));
                }
            }
            case "debuff_all" -> {
                if (damageTarget != null && parts.length >= 3) {
                    CombatPreviewCalculator.applyBuff(damageTarget, parts[1],
                            CombatPreviewCalculator.parseInt(parts[2], 1));
                }
            }
            default -> {
                // Non-deterministic and non-number effects do not alter preview text.
            }
        }
    }

    private void addDamage(CombatPreviewCalculator.PreviewTotals totals, int baseDamage,
                           int repeat, AbstractEntity target) {
        CombatPreviewCalculator.addDamage(totals, baseDamage, repeat, previewPlayer,
                player, target, relicManager, environmentDamageModifier);
    }
}
