package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.action.BuffResolver;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure combat-number preview calculator used by the view layer.
 * <p>
 * This class mirrors deterministic damage and block calculation without
 * mutating combat state. Probabilistic card and relic effects are intentionally
 * excluded from previews.
 */
public final class CombatPreviewCalculator {

    private CombatPreviewCalculator() {
    }

    public static CardPreview previewCard(Card card, Player player,
                                          List<Enemy> enemies,
                                          Enemy target,
                                          RelicManager relicManager) {
        if (card == null || player == null) {
            return new CardPreview("", false);
        }

        PreviewTotals totals = new PreviewTotals();
        List<Enemy> aliveEnemies = aliveEnemies(enemies);
        PreviewEntity previewPlayer = copyEntity(player);
        AbstractEntity damageTarget = resolveCardDamageTarget(card, aliveEnemies, target);

        for (String effect : card.getEffects()) {
            if (effect == null || effect.isBlank()) continue;
            String[] parts = effect.split(":");
            String type = parts[0].toLowerCase();

            switch (type) {
                case "damage" -> {
                    if (damageTarget == null || parts.length < 2) break;
                    int baseDamage = parseInt(parts[1], 0);
                    int repeat = parts.length > 2 ? parseInt(parts[2], 1) : 1;
                    addDamage(totals, baseDamage, repeat, previewPlayer, player, damageTarget,
                            relicManager);
                }
                case "damage_all" -> {
                    if (aliveEnemies.isEmpty() || parts.length < 2) break;
                    int baseDamage = parseInt(parts[1], 0);
                    int repeat = parts.length > 2 ? parseInt(parts[2], 1) : 1;
                    AbstractEntity commonTarget = commonEnemyTarget(aliveEnemies);
                    addDamage(totals, baseDamage, repeat, previewPlayer, player, commonTarget,
                            relicManager);
                }
                case "counter" -> {
                    if (damageTarget == null || parts.length < 2) break;
                    if ("block".equalsIgnoreCase(parts[1]) && player.getBlock() > 0) {
                        addDamage(totals, player.getBlock(), 1, previewPlayer, player, damageTarget,
                                relicManager);
                    }
                }
                case "bonus_per_attack" -> {
                    if (damageTarget == null || parts.length < 2 || !totals.hasDamage()) {
                        break;
                    }
                    int bonus = parseInt(parts[1], 0);
                    int attackCount = (int) player.getHand().stream()
                            .filter(c -> c.getType() == Card.CardType.ATTACK)
                            .count();
                    addDamage(totals, bonus * attackCount, 1, previewPlayer, player, damageTarget,
                            relicManager);
                }
                case "bonus_low_hp" -> {
                    if (target == null || !target.isAlive() || parts.length < 3) break;
                    int threshold = parseInt(parts[1], 0);
                    int bonus = parseInt(parts[2], 0);
                    if (target.getHp() < threshold) {
                        addDamage(totals, bonus, 1, previewPlayer, player, damageTarget,
                                relicManager);
                    }
                }
                case "bonus_per_damage_taken" -> {
                    if (damageTarget == null || parts.length < 3) break;
                    int threshold = parseInt(parts[1], 1);
                    int bonus = parseInt(parts[2], 0);
                    int lostHp = player.getMaxHp() - player.getHp();
                    int extraDamage = (lostHp / Math.max(1, threshold)) * bonus;
                    if (extraDamage > 0) {
                        addDamage(totals, extraDamage, 1, previewPlayer, player, damageTarget,
                                relicManager);
                    }
                }
                case "escalating" -> {
                    if (damageTarget == null || card.getEscalatingBonus() <= 0) break;
                    int bonus = parseInt(parts.length > 1 ? parts[1] : "0", 0);
                    int alreadyStoredBonus = Math.max(0, card.getEscalatingBonus() - bonus);
                    if (alreadyStoredBonus > 0) {
                        addDamage(totals, alreadyStoredBonus, 1, previewPlayer, player, damageTarget,
                                relicManager);
                    }
                }
                case "block" -> {
                    if (parts.length < 2) break;
                    totals.addBlock(DamageCalculator.calculateBlock(parseInt(parts[1], 0),
                            previewPlayer));
                }
                case "block_per_target" -> {
                    if (parts.length < 2) break;
                    int baseBlock = parseInt(parts[1], 0) * aliveEnemies.size();
                    totals.addBlock(DamageCalculator.calculateBlock(baseBlock, previewPlayer));
                }
                case "buff" -> {
                    if (parts.length >= 3 && "self".equalsIgnoreCase(parts[1])) {
                        int stack = parts.length > 3 ? parseInt(parts[3], 1) : 1;
                        applyBuff(previewPlayer, parts[2], stack);
                    }
                }
                case "debuff" -> {
                    if (damageTarget != null && parts.length >= 3) {
                        applyBuff(damageTarget, parts[1], parseInt(parts[2], 1));
                    }
                }
                case "debuff_all" -> {
                    if (damageTarget != null && parts.length >= 3) {
                        applyBuff(damageTarget, parts[1], parseInt(parts[2], 1));
                    }
                }
                default -> {
                    // Non-deterministic and non-number effects do not alter preview text.
                }
            }
        }

        String description = buildCardDescription(card, totals);
        return new CardPreview(description, totals.hasPreview());
    }

    public static EnemyIntentPreview previewEnemyIntent(Enemy enemy, Player player,
                                                        List<Enemy> allies,
                                                        RelicManager relicManager,
                                                        String actionId) {
        if (enemy == null || player == null) {
            return new EnemyIntentPreview("");
        }

        EnemyIntentAmounts amounts = resolveEnemyIntentAmounts(enemy, player, allies,
                actionId);
        List<Integer> damages = new ArrayList<>();
        for (Integer baseDamage : amounts.damageHits) {
            int damage = DamageCalculator.calculateDamage(baseDamage, enemy, player);
            if (relicManager != null) {
                damage = relicManager.previewModifyDamage(damage, enemy, player);
            }
            damages.add(Math.max(0, damage));
        }

        List<Integer> blocks = new ArrayList<>();
        for (Integer baseBlock : amounts.selfBlocks) {
            blocks.add(DamageCalculator.calculateBlock(baseBlock, enemy));
        }

        return new EnemyIntentPreview(formatIntentDetail(damages, blocks),
                formatDebuffDetail(amounts.debuffs));
    }

    private static void addDamage(PreviewTotals totals, int baseDamage, int repeat,
                                  AbstractEntity calculationSource,
                                  AbstractEntity relicSource,
                                  AbstractEntity target,
                                  RelicManager relicManager) {
        int safeRepeat = Math.max(1, repeat);
        for (int i = 0; i < safeRepeat; i++) {
            int damage = DamageCalculator.calculateDamage(baseDamage, calculationSource, target);
            if (relicManager != null) {
                damage = relicManager.previewModifyDamage(damage, relicSource, target);
            }
            totals.addDamage(Math.max(0, damage));
        }
    }

    private static AbstractEntity resolveCardDamageTarget(Card card,
                                                          List<Enemy> aliveEnemies,
                                                          Enemy target) {
        if (card.getTargetType() == Card.TargetType.ALL_ENEMIES) {
            return aliveEnemies.isEmpty() ? null : commonEnemyTarget(aliveEnemies);
        }
        if (target != null && target.isAlive()) {
            return copyEntity(target);
        }
        return null;
    }

    private static List<Enemy> aliveEnemies(List<Enemy> enemies) {
        List<Enemy> alive = new ArrayList<>();
        if (enemies == null) return alive;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                alive.add(enemy);
            }
        }
        return alive;
    }

    private static AbstractEntity commonEnemyTarget(List<Enemy> aliveEnemies) {
        PreviewEntity target = new PreviewEntity("preview_enemy", "敌人");
        if (aliveEnemies.isEmpty()) {
            return target;
        }

        Set<String> commonNames = null;
        for (Enemy enemy : aliveEnemies) {
            Set<String> names = new HashSet<>();
            for (BuffHook buff : enemy.getBuffs()) {
                if (buff.getStack() > 0) {
                    names.add(buff.getBuffName());
                }
            }
            if (commonNames == null) {
                commonNames = names;
            } else {
                commonNames.retainAll(names);
            }
        }

        if (commonNames == null || commonNames.isEmpty()) {
            return target;
        }
        Enemy first = aliveEnemies.get(0);
        for (BuffHook buff : first.getBuffs()) {
            if (buff.getStack() > 0 && commonNames.contains(buff.getBuffName())) {
                target.addBuff(buff);
            }
        }
        return target;
    }

    private static PreviewEntity copyEntity(AbstractEntity entity) {
        PreviewEntity copy = new PreviewEntity(entity.getId(), entity.getName());
        for (BuffHook buff : entity.getBuffs()) {
            if (buff.getStack() > 0) {
                copy.addBuff(buff);
            }
        }
        return copy;
    }

    private static void applyBuff(AbstractEntity target, String buffName, int stack) {
        BuffHook incoming = BuffResolver.resolve(buffName).create(Math.max(1, stack));
        BuffHook existing = null;
        for (BuffHook buff : target.getBuffs()) {
            if (buff.getBuffName().equals(incoming.getBuffName())) {
                existing = buff;
                break;
            }
        }
        if (existing != null) {
            target.removeBuff(existing.getBuffName());
            target.addBuff(BuffResolver.resolve(buffName)
                    .create(existing.getStack() + Math.max(1, stack)));
        } else {
            target.addBuff(incoming);
        }
    }

    private static String buildCardDescription(Card card, PreviewTotals totals) {
        if (!totals.hasPreview()) {
            return card.getDescription();
        }

        List<String> lines = new ArrayList<>();
        if (totals.hasDamage()) {
            lines.add("造成 " + formatDamage(totals.damageHits) + " 点伤害");
        }
        if (totals.block > 0) {
            lines.add("获得 " + totals.block + " 点格挡");
        }
        return String.join("，", lines);
    }

    private static String formatIntentDetail(List<Integer> damages, List<Integer> blocks) {
        List<String> parts = new ArrayList<>();
        if (!damages.isEmpty()) {
            parts.add(formatDamage(damages));
        }
        if (!blocks.isEmpty()) {
            parts.add(String.valueOf(blocks.stream().mapToInt(Integer::intValue).sum()));
        }
        return String.join(" / ", parts);
    }

    private static String formatDebuffDetail(List<String> debuffs) {
        if (debuffs.isEmpty()) return "";
        List<String> unique = new ArrayList<>();
        for (String debuff : debuffs) {
            if (!unique.contains(debuff)) {
                unique.add(debuff);
            }
        }
        return String.join("/", unique);
    }

    private static String formatDamage(List<Integer> damageHits) {
        if (damageHits.isEmpty()) return "";
        boolean same = true;
        int first = damageHits.get(0);
        for (int damage : damageHits) {
            if (damage != first) {
                same = false;
                break;
            }
        }
        if (same && damageHits.size() > 1) {
            return first + " x " + damageHits.size();
        }
        if (damageHits.size() == 1) {
            return String.valueOf(first);
        }
        return String.valueOf(damageHits.stream().mapToInt(Integer::intValue).sum());
    }

    private static EnemyIntentAmounts resolveEnemyIntentAmounts(Enemy enemy, Player player,
                                                                List<Enemy> allies,
                                                                String actionId) {
        EnemyIntentAmounts amounts = new EnemyIntentAmounts();
        if (actionId == null || actionId.isBlank()) return amounts;

        String basic = actionId.toLowerCase();
        if (basic.startsWith("atk")) {
            if (addAttackDsl(amounts, actionId.substring(3))) {
                return amounts;
            }
        }
        if (basic.startsWith("attack")) {
            if (addAttackDsl(amounts, actionId.substring(6))) {
                return amounts;
            }
        }
        if (basic.startsWith("def")) {
            if (addBlockDsl(amounts, actionId.substring(3))) {
                return amounts;
            }
        }
        if (basic.startsWith("block")) {
            if (addBlockDsl(amounts, actionId.substring(5))) {
                return amounts;
            }
        }

        switch (actionId) {
            case "atk_double_3" -> addRepeatedDamage(amounts, 3, 2);
            case "atk_debuff_blockred" -> {
                addRepeatedDamage(amounts, 5, 1);
                amounts.debuffs.add("易碎");
            }
            case "atk_debuff_fragile" -> {
                addRepeatedDamage(amounts, 5, 1);
                amounts.debuffs.add("脆弱");
            }
            case "def_fly" -> amounts.selfBlocks.add(3);
            case "atk_peck" -> addRepeatedDamage(amounts, 3, 1);
            case "atk_dive" -> addRepeatedDamage(amounts, 7, 1);
            case "atk_poison_3" -> {
                addRepeatedDamage(amounts, 2, 1);
                amounts.debuffs.add("中毒3");
            }
            case "def_block_5" -> amounts.selfBlocks.add(5);
            case "atk_double_wither" -> {
                addRepeatedDamage(amounts, 8, 2);
                amounts.debuffs.add("凋零");
            }
            case "atk_trigger_wither" -> {
                addRepeatedDamage(amounts, 8, 1);
                amounts.debuffs.add("凋零");
            }
            case "atk_combo_goblin", "atk_combo_chief" -> addRepeatedDamage(amounts, 8, 1);
            case "def_block_10", "def_root", "def_assemble" -> amounts.selfBlocks.add(10);
            case "inc_strength_block" -> amounts.selfBlocks.add(10);
            case "atk_barrage" -> addRepeatedDamage(amounts, 2, 5);
            case "atk_heavy" -> addRepeatedDamage(amounts, 9, 1);
            case "atk_poison_9" -> {
                addRepeatedDamage(amounts, 9, 1);
                amounts.debuffs.add("中毒3");
            }
            case "atk_precise" -> addRepeatedDamage(amounts, 10 + (player.getBlock() > 0 ? 5 : 0), 1);
            case "atk_vine" -> {
                addRepeatedDamage(amounts, 8, 1);
                amounts.debuffs.add("中毒");
            }
            case "atk_thorn" -> {
                addRepeatedDamage(amounts, 4, 4);
                amounts.debuffs.add("易碎");
            }
            case "def_harden" -> amounts.selfBlocks.add(8);
            case "def_sticky_wall" -> amounts.selfBlocks.add(3);
            case "atk_bounce" -> addRepeatedDamage(amounts, 2 + enemy.getBlock() / 2, 1);
            case "curse_mark" -> {
                addRepeatedDamage(amounts, 3, 5);
                amounts.debuffs.add("脆弱/易碎");
            }
            case "inc_strength_block_4" -> amounts.selfBlocks.add(20);
            case "atk_dual_blade" -> {
                addRepeatedDamage(amounts, 7, 2);
                amounts.debuffs.add("虚弱");
            }
            case "atk_tear" -> addRepeatedDamage(amounts, 7, 2);
            case "atk_finisher" -> addRepeatedDamage(amounts, 12, 1);
            case "atk_spray" -> {
                addRepeatedDamage(amounts, 5, 3);
                amounts.debuffs.add("虚弱");
            }
            case "atk_rot" -> {
                addRepeatedDamage(amounts, 6, 1);
                amounts.debuffs.add("中毒2");
            }
            case "atk_thorn_storm" -> addRepeatedDamage(amounts, 4, 7);
            case "atk_wither_strike", "atk_wither_combo" -> {
                addRepeatedDamage(amounts, 15, 1);
                amounts.debuffs.add("凋零");
            }
            case "atk_filament" -> {
                addRepeatedDamage(amounts, 7, 1);
                amounts.debuffs.add("中毒2");
            }
            case "atk_spore_storm" -> {
                addRepeatedDamage(amounts, 5, 4);
                amounts.debuffs.add("中毒");
            }
            case "atk_symbiosis" -> addRepeatedDamage(amounts, 10, 1);
            case "atk_horizontal_slash", "atk_sentinel_spear" -> addRepeatedDamage(amounts, 18, 1);
            case "atk_wild_dance" -> addRepeatedDamage(amounts, 6, 3);
            case "def_block_8", "def_chaos_8" -> amounts.selfBlocks.add(8);
            case "atk_fog_blade" -> addRepeatedDamage(amounts, 2, 10);
            case "atk_yin_wind" -> {
                addRepeatedDamage(amounts, 12, 1);
                amounts.debuffs.add("脆弱");
            }
            case "atk_peck_12", "atk_overload_shot" -> addRepeatedDamage(amounts, 12, 1);
            case "atk_phantom_strike" -> addRepeatedDamage(amounts, 3, 4);
            case "atk_phantom_dance" -> {
                addRepeatedDamage(amounts, 5, 2);
                amounts.debuffs.add("凋零");
            }
            case "buff_dance", "def_block_12", "def_sword_shield", "def_magic_shield" ->
                    amounts.selfBlocks.add(12);
            case "atk_wing_slap" -> addRepeatedDamage(amounts, 4, 2);
            case "atk_landslide" -> {
                addRepeatedDamage(amounts, 8, 1);
                amounts.selfBlocks.add(3);
            }
            case "atk_gravel" -> addRepeatedDamage(amounts, 9, 2);
            case "def_cliff" -> amounts.selfBlocks.add(10);
            case "atk_random_strike" -> addRepeatedDamage(amounts, 8, 3);
            case "atk_double_strike" -> addRepeatedDamage(amounts, 10, 2);
            case "def_formation" -> amounts.selfBlocks.add(10);
            case "def_iron_wall" -> amounts.selfBlocks.add(15 * aliveEnemyCount(allies));
            case "atk_shot" -> addRepeatedDamage(amounts, 8, 1);
            case "atk_dual_blade_16" -> addRepeatedDamage(amounts, 16, 2);
            case "atk_thrust", "atk_stone_fist" -> addRepeatedDamage(amounts, 28, 1);
            case "def_iron_wall_30" -> amounts.selfBlocks.add(30);
            case "atk_shield_bash" -> addRepeatedDamage(amounts, Math.max(1, enemy.getBlock() / 2), 1);
            case "atk_shock" -> {
                addRepeatedDamage(amounts, 10 + enemy.getBlock() / 10, 1);
                amounts.selfBlocks.add(10);
            }
            case "atk_spread_shot" -> {
                addRepeatedDamage(amounts, 4, 5);
                amounts.debuffs.add("脆弱");
            }
            case "atk_point_shot" -> {
                addRepeatedDamage(amounts, 3, 7);
                amounts.debuffs.add("易碎");
            }
            case "atk_magic_missile" -> addRepeatedDamage(amounts, 2, 5);
            case "atk_arcane_blast" -> addRepeatedDamage(amounts, 3, 8);
            case "def_stone_skin" -> amounts.selfBlocks.add(20);
            case "atk_claw" -> {
                addRepeatedDamage(amounts, 8, 3);
                amounts.debuffs.add("易碎");
            }
            case "atk_command" -> addRepeatedDamage(amounts, 5, 3);
            case "def_team_shield" -> amounts.selfBlocks.add(5);
            case "def_stand_firm" -> amounts.selfBlocks.add(15);
            case "atk_shadow_strike", "transfer_curse" -> addRepeatedDamage(amounts, 20, 1);
            case "atk_shadow_burst" -> addRepeatedDamage(amounts, 36, 1);
            case "atk_shadow_dance" -> addRepeatedDamage(amounts, 16, 2);
            case "atk_demon_rage" -> {
                addRepeatedDamage(amounts, 9, 3);
                amounts.selfBlocks.add(20);
            }
            case "def_demon_barrier" -> amounts.selfBlocks.add(40);
            case "charge_attack" -> {
                if (hasBuff(enemy, "Strength")) {
                    addRepeatedDamage(amounts, 30, 1);
                    amounts.debuffs.add("凋零");
                }
            }
            case "atk_demon_dance" -> {
                addRepeatedDamage(amounts, 8, 4);
                amounts.debuffs.add("凋零");
            }
            case "def_puppet_repair" -> amounts.selfBlocks.add(20);
            case "atk_dark_energy" -> addRepeatedDamage(amounts, 14, 3);
            case "steal_block" -> {
                if (player.getBlock() > 0) amounts.selfBlocks.add(player.getBlock());
            }
            case "multi_curse" -> {
                amounts.selfBlocks.add(10);
                amounts.debuffs.add("虚弱/凋零/脆弱");
            }
            case "def_shield_puppet" -> amounts.selfBlocks.add(10);
            case "shadow_assault" -> {
                addRepeatedDamage(amounts, 5, 1);
                amounts.debuffs.add("眩晕");
            }
            default -> {
                addDebuffsFromActionName(amounts, actionId);
            }
        }
        return amounts;
    }

    private static boolean addAttackDsl(EnemyIntentAmounts amounts, String numPart) {
        String normalized = trimPrefixUnderscores(numPart);
        if (normalized.contains("x")) {
            String[] parts = normalized.split("x");
            if (parts.length < 2 || !isInteger(parts[0]) || !isInteger(parts[1])) {
                return false;
            }
            addRepeatedDamage(amounts, parseInt(parts[0], 0),
                    parseInt(parts[1], 1));
            return true;
        }
        if (!isInteger(normalized)) return false;
        addRepeatedDamage(amounts, parseInt(normalized, 0), 1);
        return true;
    }

    private static boolean addBlockDsl(EnemyIntentAmounts amounts, String numPart) {
        String normalized = trimPrefixUnderscores(numPart);
        if (normalized.startsWith("block_")) {
            normalized = normalized.substring("block_".length());
        }
        if (!isInteger(normalized)) return false;
        amounts.selfBlocks.add(parseInt(normalized, 0));
        return true;
    }

    private static String trimPrefixUnderscores(String value) {
        String normalized = value != null ? value : "";
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static void addRepeatedDamage(EnemyIntentAmounts amounts, int damage, int count) {
        for (int i = 0; i < Math.max(1, count); i++) {
            if (damage > 0) {
                amounts.damageHits.add(damage);
            }
        }
    }

    private static int aliveEnemyCount(List<Enemy> enemies) {
        int count = 0;
        if (enemies == null) return 1;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static boolean hasBuff(AbstractEntity entity, String buffName) {
        for (BuffHook buff : entity.getBuffs()) {
            if (buff.getBuffName().equalsIgnoreCase(buffName) && buff.getStack() > 0) {
                return true;
            }
        }
        return false;
    }

    private static void addDebuffsFromActionName(EnemyIntentAmounts amounts, String actionId) {
        String normalized = actionId.toLowerCase();
        if (normalized.contains("poison")) amounts.debuffs.add("中毒");
        if (normalized.contains("weak")) amounts.debuffs.add("虚弱");
        if (normalized.contains("fragile")) amounts.debuffs.add("脆弱");
        if (normalized.contains("blockred")) amounts.debuffs.add("易碎");
        if (normalized.contains("wither")) amounts.debuffs.add("凋零");
        if (normalized.contains("stun")) amounts.debuffs.add("眩晕");
        if (normalized.startsWith("curse") || normalized.startsWith("debuff")) {
            amounts.debuffs.add("负面");
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean isInteger(String raw) {
        if (raw == null || raw.isBlank()) return false;
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) return false;
        }
        return true;
    }

    private static class PreviewTotals {
        private final List<Integer> damageHits = new ArrayList<>();
        private int block;

        private void addDamage(int damage) {
            damageHits.add(damage);
        }

        private void addBlock(int amount) {
            block += Math.max(0, amount);
        }

        private boolean hasDamage() {
            return !damageHits.isEmpty();
        }

        private boolean hasPreview() {
            return hasDamage() || block > 0;
        }
    }

    private static class EnemyIntentAmounts {
        private final List<Integer> damageHits = new ArrayList<>();
        private final List<Integer> selfBlocks = new ArrayList<>();
        private final List<String> debuffs = new ArrayList<>();
    }

    private static class PreviewEntity extends AbstractEntity {
        private PreviewEntity(String id, String name) {
            super(id, name, 1);
        }
    }
}
