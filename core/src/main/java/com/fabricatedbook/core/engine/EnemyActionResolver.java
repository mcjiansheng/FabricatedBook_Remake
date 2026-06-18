package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.*;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * EnemyActionResolver — 敌人行动名称解析器
 * <p>
 * 将原版 C 项目的敌人行动名称（如 atk_double_3, inc_strength_3 等）
 * 映射为当前 CombatEngine 可以执行的 CombatAction 列表。
 * 负责解析所有 level1~5.json 中出现的 actionScript 名称，
 * 使 CombatEngine 不再 fallback 到默认攻击。
 * <p>
 * 引用方：CombatEngine.parseEnemyAction
 */
public final class EnemyActionResolver {

    private EnemyActionResolver() {}

    /**
     * 返回接近杀戮尖塔风格的敌人意图文案。
     * 这里只描述玩家需要预判的信息，不执行任何动作。
     */
    public static String describeIntent(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return "未知";
        }

        String basic = describeBasicDsl(actionId);
        if (basic != null) return basic;

        return switch (actionId) {
            case "atk_double_3" -> "攻击 2 × 3";
            case "atk_debuff_blockred" -> "攻击 5 / 削弱";
            case "def_fly" -> "防御 3";
            case "atk_peck" -> "攻击 3";
            case "atk_dive" -> "攻击 7";
            case "atk_poison_3" -> "攻击 2 / 削弱";
            case "atk_debuff_fragile" -> "攻击 5 / 削弱";
            case "def_block_5" -> "防御 5";
            case "buff_resist_strength" -> "强化";
            case "atk_double_wither" -> "攻击 2 × 8 / 削弱";
            case "curse_debuffs" -> "削弱";
            case "atk_trigger_wither" -> "攻击 8 / 削弱";
            case "def_block_10" -> "防御 10";
            case "inc_strength_block" -> "强化 / 防御 10";
            case "atk_barrage" -> "攻击 5 × 2";
            case "atk_heavy" -> "攻击 9 / 强化";
            case "atk_precise" -> "攻击 10+";
            case "atk_vine" -> "攻击 8 / 削弱";
            case "atk_thorn" -> "攻击 4 × 4";
            case "def_root" -> "防御 10";
            case "heal_all_5" -> "回复 5";
            case "atk_poison_blade" -> "攻击 6 / 削弱";
            case "atk_combo_goblin" -> "攻击 8 / 防御 4";
            case "def_harden" -> "防御 8";
            case "def_sticky_wall" -> "群体防御 3";
            case "atk_bounce" -> "攻击";
            case "def_share_block" -> "转移格挡";
            case "curse_mark" -> "削弱 / 攻击 5 × 3";
            case "inc_strength_block_4" -> "强化 / 防御 20";
            case "atk_dual_blade" -> "攻击 2 × 7";
            case "atk_finisher" -> "攻击 12 / 强化";
            case "atk_poison_9" -> "攻击 9 / 削弱";
            case "atk_combo_chief" -> "攻击 8 / 防御 10";
            case "atk_spray" -> "攻击 3 × 5 / 削弱";
            case "def_assemble" -> "防御 10 / 回复";
            case "atk_rot" -> "攻击 6 / 削弱";
            case "atk_tear" -> "攻击 2 × 7";
            case "self_fragile" -> "自损";
            case "atk_berserk" -> "攻击 12-20";
            case "heal_self" -> "回复";
            case "atk_wither_strike" -> "攻击 15 / 削弱";
            case "atk_thorn_storm" -> "攻击 7 × 4";
            case "idle", "stun" -> "无行动";
            default -> {
                if (actionId.startsWith("buff") || actionId.startsWith("inc")) {
                    yield "强化";
                }
                if (actionId.startsWith("debuff") || actionId.startsWith("curse")) {
                    yield "削弱";
                }
                if (actionId.startsWith("heal")) {
                    yield "回复 " + actionId.substring(4);
                }
                yield "未知";
            }
        };
    }

    private static String describeBasicDsl(String actionId) {
        try {
            if (actionId.startsWith("atk")) {
                String numPart = actionId.substring(3);
                return describeAttackNumber(numPart);
            }
            if (actionId.startsWith("attack")) {
                String numPart = actionId.substring(6);
                return describeAttackNumber(numPart);
            }
            if (actionId.startsWith("def")) {
                return describeBlockNumber(actionId.substring(3));
            }
            if (actionId.startsWith("block")) {
                return describeBlockNumber(actionId.substring(5));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String describeBlockNumber(String numPart) {
        if (numPart == null || numPart.isBlank()) return null;
        String normalized = numPart;
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("block_")) {
            normalized = normalized.substring("block_".length());
        }
        if (!isInteger(normalized)) return null;
        return "防御 " + normalized;
    }

    private static String describeAttackNumber(String numPart) {
        if (numPart == null || numPart.isBlank()) return null;
        if (numPart.contains("x")) {
            String[] parts = numPart.split("x");
            if (isInteger(parts[0]) && isInteger(parts[1])) {
                return "攻击 " + parts[0] + " × " + parts[1];
            }
            return null;
        }
        if (!isInteger(numPart)) return null;
        return "攻击 " + numPart;
    }

    private static boolean isInteger(String value) {
        if (value == null || value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    /**
     * 解析敌人行动名称并生成 CombatAction 列表。
     *
     * @param enemy    执行动作的敌人
     * @param actionId 行动标识
     * @param player   玩家
     * @param allies   友方敌人列表
     * @param random   随机数
     * @return 动作列表
     */
    public static List<CombatAction> resolve(Enemy enemy, String actionId,
                                              Player player, List<Enemy> allies,
                                              Random random) {
        List<CombatAction> actions = new ArrayList<>();
        if (actionId == null) return actions;

        try {
            switch (actionId) {
                // ===== 拾荒者 (Scavenger) =====
                case "atk_double_3": // 2×3 damage
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 3, true));
                    break;
                case "inc_strength_3": // gain 3 strength
                    actions.add(new ApplyBuffAction(enemy, "Strength", 3));
                    break;
                case "atk_debuff_blockred": // attack 5 + BlockReduction
                    actions.add(new DamageAction(enemy, List.of(player), 5));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 1));
                    break;

                // ===== 秃鹫 (Vulture) =====
                case "def_fly": // gain 3 block
                    actions.add(new GainBlockAction(enemy, 3));
                    break;
                case "atk_peck": // attack 3
                    actions.add(new DamageAction(enemy, List.of(player), 3));
                    break;
                case "atk_dive": // attack 7
                    actions.add(new DamageAction(enemy, List.of(player), 7));
                    break;

                // ===== 盗贼 (Thief) =====
                case "atk_poison_3": // attack 2 + poison 3
                    actions.add(new DamageAction(enemy, List.of(player), 2));
                    actions.add(new ApplyBuffAction(player, "Poison", 3));
                    break;
                case "atk_debuff_fragile": // attack 5 + Fragile 2
                    actions.add(new DamageAction(enemy, List.of(player), 5));
                    actions.add(new ApplyBuffAction(player, "Fragile", 2));
                    break;
                case "def_block_5": // block 5
                    actions.add(new GainBlockAction(enemy, 5));
                    break;

                // ===== 孤魂野鬼 (Lone Ghost) =====
                case "buff_resist_strength": // gain 5 Resistance + 5 Strength + heal 10
                    actions.add(new ApplyBuffAction(enemy, "Resistance", 5));
                    actions.add(new ApplyBuffAction(enemy, "Strength", 5));
                    actions.add(new HealAction(enemy, 10));
                    break;
                case "atk_double_wither": // 8×2, each 50% withering 1
                    for (int i = 0; i < 2; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 8, true));
                        if (random.nextInt(100) < 50)
                            actions.add(new ApplyBuffAction(player, "Withering", 1));
                    }
                    break;
                case "curse_debuffs": // player gets 5 Fragile + 5 Weak
                    actions.add(new ApplyBuffAction(player, "Fragile", 5));
                    actions.add(new ApplyBuffAction(player, "Weak", 5));
                    break;
                case "atk_trigger_wither": // attack 8 + trigger withering
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    triggerWithering(actions, player, 1);
                    break;
                case "def_block_10": // block 10
                    actions.add(new GainBlockAction(enemy, 10));
                    break;

                // ===== 猎人 (Hunter) =====
                case "inc_strength_block": // Strength 3 + Block 10
                    actions.add(new ApplyBuffAction(enemy, "Strength", 3));
                    actions.add(new GainBlockAction(enemy, 10));
                    break;
                case "atk_barrage": // 2×5, each 25% Weak 1
                    for (int i = 0; i < 5; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 2, true));
                        if (random.nextInt(100) < 25)
                            actions.add(new ApplyBuffAction(player, "Weak", 1));
                    }
                    break;
                case "atk_heavy": // attack 9 + BlockIncrease 3
                    actions.add(new DamageAction(enemy, List.of(player), 9));
                    actions.add(new ApplyBuffAction(enemy, "BlockIncrease", 3));
                    break;
                case "atk_precise": // attack 10 (+5 if player has block)
                    int preciseDmg = 10 + (player.getBlock() > 0 ? 5 : 0);
                    actions.add(new DamageAction(enemy, List.of(player), preciseDmg));
                    break;

                // ===== 树人 (Treeman) =====
                case "atk_vine": // attack 8, 25% Poison 3
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    if (random.nextInt(100) < 25)
                        actions.add(new ApplyBuffAction(player, "Poison", 3));
                    break;
                case "atk_thorn": // 4×4, each 50% BlockReduction 1
                    for (int i = 0; i < 4; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 4, true));
                        if (random.nextInt(100) < 50)
                            actions.add(new ApplyBuffAction(player, "BlockReduction", 1));
                    }
                    break;
                case "def_root": // block 10, if hp<30 then Armor
                    actions.add(new GainBlockAction(enemy, 10));
                    if (enemy.getHp() < 30)
                        actions.add(new ApplyBuffAction(enemy, "Armor", 1));
                    break;

                // ===== 哥布林 (Goblin) =====
                case "heal_all_5": // heal all allies 5
                    actions.add(new HealAction(enemy, 5));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new HealAction(ally, 5));
                    }
                    break;
                case "atk_poison_blade": // attack 6 + Poison 3
                    actions.add(new DamageAction(enemy, List.of(player), 6));
                    actions.add(new ApplyBuffAction(player, "Poison", 3));
                    break;
                case "atk_combo_goblin": // attack 8 + block 4
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    actions.add(new GainBlockAction(enemy, 4));
                    break;

                // ===== 史莱姆 (Slime) =====
                case "def_harden": // block 8
                    actions.add(new GainBlockAction(enemy, 8));
                    break;
                case "def_sticky_wall": // block 3 to all allies
                    for (Enemy ally : allies) {
                        if (ally.isAlive())
                            actions.add(new GainBlockAction(ally, 3));
                    }
                    break;
                case "atk_bounce": // damage = 2 + 50% block
                    int bounceDmg = 2 + enemy.getBlock() / 2;
                    actions.add(new DamageAction(enemy, List.of(player), bounceDmg));
                    break;
                case "def_share_block": // give own block to all allies
                    int sharedBlock = enemy.getBlock();
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive() && sharedBlock > 0)
                            actions.add(new GainBlockAction(ally, sharedBlock));
                    }
                    break;

                // ===== 老猎人 (Old Hunter) =====
                case "curse_mark": // Fragile 3 + BlockReduction 3 + attack 3×5
                    actions.add(new ApplyBuffAction(player, "Fragile", 3));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 3));
                    for (int i = 0; i < 5; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 3, true));
                    break;
                case "inc_strength_block_4": // Strength 4 + Block 20
                    actions.add(new ApplyBuffAction(enemy, "Strength", 4));
                    actions.add(new GainBlockAction(enemy, 20));
                    break;
                case "atk_dual_blade": // 7×2, each 50% Weak 1
                    for (int i = 0; i < 2; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 7, true));
                        if (random.nextInt(100) < 50)
                            actions.add(new ApplyBuffAction(player, "Weak", 1));
                    }
                    break;
                case "atk_finisher": // attack 12 + BlockIncrease 2
                    actions.add(new DamageAction(enemy, List.of(player), 12));
                    actions.add(new ApplyBuffAction(enemy, "BlockIncrease", 2));
                    break;

                // ===== 哥布林统领 (Goblin Chief) =====
                case "atk_poison_9": // attack 9 + Poison 3
                    actions.add(new DamageAction(enemy, List.of(player), 9));
                    actions.add(new ApplyBuffAction(player, "Poison", 3));
                    break;
                case "atk_combo_chief": // attack 8 + block 10
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    actions.add(new GainBlockAction(enemy, 10));
                    break;
                case "atk_spray": // 5×3 + Weak 2
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 5, true));
                    actions.add(new ApplyBuffAction(player, "Weak", 2));
                    break;
                case "def_assemble": // block 10 + heal all 7
                    actions.add(new GainBlockAction(enemy, 10));
                    actions.add(new HealAction(enemy, 7));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new HealAction(ally, 7));
                    }
                    break;

                // ===== 腐尸 (Corpse) =====
                case "atk_rot": // attack 6 + Poison 2
                    actions.add(new DamageAction(enemy, List.of(player), 6));
                    actions.add(new ApplyBuffAction(player, "Poison", 2));
                    break;
                case "atk_tear": // 7×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 7, true));
                    break;
                case "self_fragile": // self Fragile 5
                    actions.add(new ApplyBuffAction(enemy, "Fragile", 5));
                    break;

                // ===== 堕落守林人 (Fallen Warden) =====
                case "atk_berserk": // random 12-20 damage
                    int berserkDmg = 12 + random.nextInt(9);
                    actions.add(new DamageAction(enemy, List.of(player), berserkDmg));
                    break;

                // ===== Boss: 凋零之树 (Withering Tree) =====
                case "heal_self": // heal 10-25
                    int healAmt = 10 + random.nextInt(16);
                    actions.add(new HealAction(enemy, healAmt));
                    break;
                case "atk_wither_strike": // attack 15 + Withering 3
                    actions.add(new DamageAction(enemy, List.of(player), 15));
                    actions.add(new ApplyBuffAction(player, "Withering", 3));
                    break;
                case "atk_thorn_storm": // 4×7 + block 5
                    for (int i = 0; i < 7; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 4, true));
                    actions.add(new GainBlockAction(enemy, 5));
                    break;
                case "trigger_wither_self": // trigger withering + self Resistance 3
                    triggerWithering(actions, player, 1);
                    actions.add(new ApplyBuffAction(enemy, "Resistance", 3));
                    break;
                case "atk_wither_combo": // attack 15 + trigger withering
                    actions.add(new DamageAction(enemy, List.of(player), 15));
                    triggerWithering(actions, player, 1);
                    break;
                case "life_drain": // trigger withering + heal by damage dealt
                    actions.add(new WitheringDrainAction(enemy, player));
                    break;

                // ===== Boss: 人中菌 (Human Fungus) =====
                case "atk_filament": // attack 7 + Poison 2
                    actions.add(new DamageAction(enemy, List.of(player), 7));
                    actions.add(new ApplyBuffAction(player, "Poison", 2));
                    break;
                case "atk_spore_storm": // 5×4, each 25% increase poison
                    for (int i = 0; i < 4; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 5, true));
                        if (random.nextInt(100) < 25)
                            actions.add(new ApplyBuffAction(player, "Poison", 2));
                    }
                    break;
                case "atk_symbiosis": // attack 10 + heal by player poison count/3
                    int poisonCount = getBuffStack(player, "Poison");
                    actions.add(new DamageAction(enemy, List.of(player), 10));
                    if (poisonCount >= 3)
                        actions.add(new HealAction(enemy, poisonCount / 3));
                    break;
                case "curse_poison_fragile": // Poison 3 + Fragile 3
                    actions.add(new ApplyBuffAction(player, "Poison", 3));
                    actions.add(new ApplyBuffAction(player, "Fragile", 3));
                    break;

                // ===== Boss: 迷失的守林人 (Lost Warden) =====
                case "atk_horizontal_slash": // attack 18
                    actions.add(new DamageAction(enemy, List.of(player), 18));
                    break;
                case "atk_wild_dance": // 6×3
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 6, true));
                    break;
                case "curse_fragile_both": // Fragile 3 to self and player
                    actions.add(new ApplyBuffAction(enemy, "Fragile", 3));
                    actions.add(new ApplyBuffAction(player, "Fragile", 3));
                    break;
                case "def_block_8": // block 8
                    actions.add(new GainBlockAction(enemy, 8));
                    break;
                case "idle_stare": // do nothing
                    break;

                // ===== 腐化的守林人 (Corrupted Warden) =====
                case "atk_chaos": // random 13-20 damage
                    int chaosDmg = 13 + random.nextInt(8);
                    actions.add(new DamageAction(enemy, List.of(player), chaosDmg));
                    break;
                case "def_chaos": // random 8-20 block
                    int chaosBlock = 8 + random.nextInt(13);
                    actions.add(new GainBlockAction(enemy, chaosBlock));
                    break;

                // ===== 雾鬼 (Fog Ghost) =====
                case "buff_strength_3": // Strength 3
                    actions.add(new ApplyBuffAction(enemy, "Strength", 3));
                    break;
                case "atk_fog_blade": // 2×10
                    for (int i = 0; i < 10; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 2, true));
                    break;
                case "atk_yin_wind": // attack 12 + Fragile 2
                    actions.add(new DamageAction(enemy, List.of(player), 12));
                    actions.add(new ApplyBuffAction(player, "Fragile", 2));
                    break;

                // ===== 幻影 (Phantom) =====
                case "atk_phantom_strike": // 3×4
                    for (int i = 0; i < 4; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 3, true));
                    break;
                case "curse_weak_2": // Weak 2
                    actions.add(new ApplyBuffAction(player, "Weak", 2));
                    break;
                case "atk_phantom_dance": // 5×2, each 10% trigger withering
                    for (int i = 0; i < 2; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), 5, true));
                        if (random.nextInt(100) < 10)
                            triggerWithering(actions, player, 1);
                    }
                    break;

                // ===== 雾行鸟 (Fog Walker) =====
                case "atk_peck_12": // attack 12
                    actions.add(new DamageAction(enemy, List.of(player), 12));
                    break;
                case "buff_dance": // Strength 2 + Block 12
                    actions.add(new ApplyBuffAction(enemy, "Strength", 2));
                    actions.add(new GainBlockAction(enemy, 12));
                    break;
                case "atk_wing_slap": // 4×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 4, true));
                    break;

                // ===== 腐烂山鬼 (Rotten Mountain Ghost) =====
                case "atk_landslide": // attack 8 + block 3
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    actions.add(new GainBlockAction(enemy, 3));
                    break;
                case "atk_gravel": // 9×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 9, true));
                    break;
                case "def_bedrock": // Resistance 3
                    actions.add(new ApplyBuffAction(enemy, "Resistance", 3));
                    break;
                case "def_cliff": // block 10
                    actions.add(new GainBlockAction(enemy, 10));
                    break;

                // ===== 迷途旅人 (Lost Traveler) =====
                case "atk_random_strike": // 8×3
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 8, true));
                    break;
                case "atk_double_strike": // 10×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 10, true));
                    break;
                case "def_block_12": // block 12
                    actions.add(new GainBlockAction(enemy, 12));
                    break;
                case "buff_awaken": // Strength 2 + Resistance 2
                    actions.add(new ApplyBuffAction(enemy, "Strength", 2));
                    actions.add(new ApplyBuffAction(enemy, "Resistance", 2));
                    break;

                // ===== Boss: 最后的哨兵 (The Last Sentinel) =====
                case "atk_sentinel_spear": // attack 18
                    actions.add(new DamageAction(enemy, List.of(player), 18));
                    break;
                case "def_formation": // block 10 to self and turrets
                    actions.add(new GainBlockAction(enemy, 10));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new GainBlockAction(ally, 10));
                    }
                    break;
                case "def_iron_wall": // block 15 per alive enemy
                    int ironBlock = 15 * (1 + (int) allies.stream().filter(e -> e != enemy && e.isAlive()).count());
                    actions.add(new GainBlockAction(enemy, ironBlock));
                    break;
                case "heal_repair": // heal 7 to self and turrets
                    actions.add(new HealAction(enemy, 7));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new HealAction(ally, 7));
                    }
                    break;
                case "heal_full_repair": // heal turrets 20
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new HealAction(ally, 20));
                    }
                    break;
                // 炮塔 actions
                case "atk_shot": // attack 8
                    actions.add(new DamageAction(enemy, List.of(player), 8));
                    break;
                case "atk_overload_shot": // attack 12 + self damage 5
                    actions.add(new DamageAction(enemy, List.of(player), 12));
                    enemy.takeDamage(5);
                    break;

                // ===== 剑卫 (Sword Guardian) =====
                case "atk_dual_blade_16": // 16×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 16, true));
                    break;
                case "def_sword_shield": // block 12
                    actions.add(new GainBlockAction(enemy, 12));
                    break;
                case "atk_thrust": // attack 28
                    actions.add(new DamageAction(enemy, List.of(player), 28));
                    break;

                // ===== 盾卫 (Shield Guardian) =====
                case "def_iron_wall_30": // block 30 + Armor
                    actions.add(new GainBlockAction(enemy, 30));
                    actions.add(new ApplyBuffAction(enemy, "Armor", 1));
                    break;
                case "atk_shield_bash": // 50% block as damage, 50% self stun
                    int bashDmg = enemy.getBlock() / 2;
                    actions.add(new DamageAction(enemy, List.of(player), Math.max(1, bashDmg)));
                    if (random.nextInt(100) < 50)
                        enemy.setDizzy(true);
                    break;
                case "atk_shock": // attack 10 + 10% block + gain 10 block
                    int shockDmg = 10 + enemy.getBlock() / 10;
                    actions.add(new DamageAction(enemy, List.of(player), shockDmg));
                    actions.add(new GainBlockAction(enemy, 10));
                    break;
                case "def_team_protection": // Resistance 5 to all allies
                    actions.add(new ApplyBuffAction(enemy, "Resistance", 5));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new ApplyBuffAction(ally, "Resistance", 5));
                    }
                    break;

                // ===== 铳卫 (Gun Guardian) =====
                case "atk_spread_shot": // 4×5 + Fragile 2 + self stun
                    for (int i = 0; i < 5; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 4, true));
                    actions.add(new ApplyBuffAction(player, "Fragile", 2));
                    enemy.setDizzy(true);
                    break;
                case "atk_point_shot": // 3×7 + BlockReduction 2 + self stun
                    for (int i = 0; i < 7; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 3, true));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 2));
                    enemy.setDizzy(true);
                    break;

                // ===== 法师 (Mage) =====
                case "atk_magic_missile": // 2×5
                    for (int i = 0; i < 5; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 2, true));
                    break;
                case "atk_arcane_blast": // 3×8
                    for (int i = 0; i < 8; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 3, true));
                    break;
                case "def_magic_shield": // block 12 + heal 20
                    actions.add(new GainBlockAction(enemy, 12));
                    actions.add(new HealAction(enemy, 20));
                    break;
                case "curse_wither": // Withering 3 to player
                    actions.add(new ApplyBuffAction(player, "Withering", 3));
                    break;

                // ===== 石像鬼 (Gargoyle) =====
                case "def_stone_skin": // block 20
                    actions.add(new GainBlockAction(enemy, 20));
                    break;
                case "atk_claw": // 8×3 + BlockReduction 2
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 8, true));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 2));
                    break;
                case "atk_stone_fist": // attack 28
                    actions.add(new DamageAction(enemy, List.of(player), 28));
                    break;
                case "buff_activate": // Strength 2
                    actions.add(new ApplyBuffAction(enemy, "Strength", 2));
                    break;

                // ===== 卫士统领 (Guardian Commander) =====
                case "atk_command": // 5×3
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 5, true));
                    break;
                case "def_team_shield": // block 5 to all allies
                    for (Enemy ally : allies) {
                        if (ally.isAlive())
                            actions.add(new GainBlockAction(ally, 5));
                    }
                    break;
                case "def_stand_firm": // block 15
                    actions.add(new GainBlockAction(enemy, 15));
                    break;
                case "heal_command": // heal 5 to all allies
                    for (Enemy ally : allies) {
                        if (ally.isAlive())
                            actions.add(new HealAction(ally, 5));
                    }
                    break;
                case "heal_emergency": // stun one ally + heal 20
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive()) {
                            ally.setDizzy(true);
                            actions.add(new HealAction(ally, 20));
                            break;
                        }
                    }
                    break;

                // ===== 影卫 (Shadow Guard) =====
                case "atk_shadow_strike": // self Fragile 5 + attack 20
                    actions.add(new ApplyBuffAction(enemy, "Fragile", 5));
                    actions.add(new DamageAction(enemy, List.of(player), 20));
                    break;
                case "atk_shadow_burst": // attack 36
                    actions.add(new DamageAction(enemy, List.of(player), 36));
                    break;
                case "curse_shadow": // Fragile 4 + BlockReduction 4 to player
                    actions.add(new ApplyBuffAction(player, "Fragile", 4));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 4));
                    break;
                case "atk_shadow_dance": // 16×2
                    for (int i = 0; i < 2; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 16, true));
                    break;

                // ===== Boss: 魔王 (Demon King) =====
                case "atk_demon_rage": // 9×3 + block 20
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 9, true));
                    actions.add(new GainBlockAction(enemy, 20));
                    break;
                case "def_demon_barrier": // purify self + block 40
                    purifyEnemy(actions, enemy);
                    actions.add(new GainBlockAction(enemy, 40));
                    break;
                case "charge_attack": // context-dependent
                    boolean hasStrength = getBuffStack(enemy, "Strength") > 0;
                    if (!hasStrength) {
                        // stun 2 turns + gain 5 Strength
                        enemy.setDizzy(true);
                        actions.add(new ApplyBuffAction(enemy, "Strength", 5));
                    } else {
                        actions.add(new DamageAction(enemy, List.of(player), 30));
                        actions.add(new ApplyBuffAction(player, "Withering", 2));
                    }
                    break;
                case "atk_demon_dance": // 8×4, 20% miss, hit=Withering 2
                    for (int i = 0; i < 4; i++) {
                        if (random.nextInt(100) >= 20) { // 80% hit
                            actions.add(new DamageAction(enemy, List.of(player), 8, true));
                            actions.add(new ApplyBuffAction(player, "Withering", 2));
                        }
                    }
                    break;
                case "buff_demon_pressure": // BlockIncrease 5 to self + BlockReduction 5 to player
                    actions.add(new ApplyBuffAction(enemy, "BlockIncrease", 5));
                    actions.add(new ApplyBuffAction(player, "BlockReduction", 5));
                    break;

                // ===== Boss: 幕后黑手 (Puppetmaster) =====
                case "def_puppet_repair": // block 20 + fully heal puppet
                    actions.add(new GainBlockAction(enemy, 20));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive() && ally.getName().contains("傀儡")) {
                            int missingHp = ally.getMaxHp() - ally.getHp();
                            if (missingHp > 0)
                                actions.add(new HealAction(ally, missingHp));
                        }
                    }
                    break;
                case "atk_dark_energy": // 14×3
                    for (int i = 0; i < 3; i++)
                        actions.add(new DamageAction(enemy, List.of(player), 14, true));
                    break;
                case "steal_block": // steal player block + Armor 3
                    int stolenBlock = player.getBlock();
                    if (stolenBlock > 0) {
                        player.clearBlock();
                        actions.add(new GainBlockAction(enemy, stolenBlock));
                    }
                    actions.add(new ApplyBuffAction(enemy, "Armor", 3));
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive())
                            actions.add(new ApplyBuffAction(ally, "Armor", 3));
                    }
                    break;
                case "wither_drain": // Withering 5 + trigger + heal
                    actions.add(new ApplyBuffAction(player, "Withering", 5));
                    actions.add(new WitheringDrainAction(enemy, player));
                    break;
                case "transfer_curse": // transfer debuffs to puppet + attack 20
                    transferPlayerDebuffsToPuppet(actions, player, allies);
                    actions.add(new DamageAction(enemy, List.of(player), 20));
                    break;
                case "multi_curse": // Weak 3 + Withering 3 + Fragile 3 + block 10
                    actions.add(new ApplyBuffAction(player, "Weak", 3));
                    actions.add(new ApplyBuffAction(player, "Withering", 3));
                    actions.add(new ApplyBuffAction(player, "Fragile", 3));
                    actions.add(new GainBlockAction(enemy, 10));
                    break;
                case "shadow_assault": // attack 5, 50% stun
                    actions.add(new DamageAction(enemy, List.of(player), 5));
                    if (random.nextInt(100) < 50)
                        player.setDizzy(true);
                    break;

                // ===== 傀儡 (Puppet) =====
                case "sacrifice": // lose 10 HP, heal master 10
                    enemy.takeDamage(10);
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive()) {
                            actions.add(new HealAction(ally, 10));
                            break;
                        }
                    }
                    break;
                case "trigger_wither_puppet": // trigger withering
                    triggerWithering(actions, player, 1);
                    break;
                case "def_shield_puppet": // block 10 to master
                    for (Enemy ally : allies) {
                        if (ally != enemy && ally.isAlive()) {
                            actions.add(new GainBlockAction(ally, 10));
                            break;
                        }
                    }
                    break;

                default:
                    // Fallback: try basic DSL parsing handled by CombatEngine
                    return null;
            }
        } catch (Exception e) {
            System.err.println("[EnemyActionResolver] 解析敌人动作失败: " + actionId
                    + " - " + e.getMessage());
            return null;
        }

        return actions;
    }

    /** 引爆目标的凋零效果 */
    private static void triggerWithering(List<CombatAction> actions,
                                          com.fabricatedbook.core.entity.AbstractEntity target,
                                          int times) {
        actions.add(new TriggerWitheringAction(target, times));
    }

    /** 获取目标的指定 Buff 层数 */
    private static int getBuffStack(com.fabricatedbook.core.entity.AbstractEntity entity,
                                     String buffName) {
        for (com.fabricatedbook.core.buff.BuffHook buff : entity.getBuffs()) {
            if (buff.getBuffName().equalsIgnoreCase(buffName)) {
                return buff.getStack();
            }
        }
        return 0;
    }

    /** 清除敌人的负面效果 */
    private static void purifyEnemy(List<CombatAction> actions, Enemy enemy) {
        for (com.fabricatedbook.core.buff.BuffHook buff :
                new ArrayList<>(enemy.getBuffs())) {
            String bName = buff.getBuffName();
            if (bName.equals("Fragile") || bName.equals("BlockReduction")
                    || bName.equals("Weak") || bName.equals("Poison")
                    || bName.equals("Withering") || bName.equals("Dizziness")) {
                enemy.removeBuff(bName);
            }
        }
    }

    private static void transferPlayerDebuffsToPuppet(List<CombatAction> actions,
                                                       Player player,
                                                       List<Enemy> allies) {
        Enemy puppet = null;
        for (Enemy ally : allies) {
            if (ally != null && ally.isAlive()
                    && "buff_master_random".equals(ally.getPassive())) {
                puppet = ally;
                break;
            }
        }
        if (puppet == null) return;

        for (com.fabricatedbook.core.buff.BuffHook buff :
                new ArrayList<>(player.getBuffs())) {
            String name = buff.getBuffName();
            if (isNegativeBuff(name) && buff.getStack() > 0) {
                actions.add(new ApplyBuffAction(puppet, name, buff.getStack()));
                player.removeBuff(name);
            }
        }
    }

    private static boolean isNegativeBuff(String buffName) {
        return buffName.equals("Fragile") || buffName.equals("BlockReduction")
                || buffName.equals("Weak") || buffName.equals("Poison")
                || buffName.equals("Withering") || buffName.equals("Dizziness");
    }
}
