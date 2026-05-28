package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * DamageCalculator — 伤害计算器
 * <p>
 * 静态工具类，负责计算经过双方 Buff 修正后的最终伤害值。
 * 结算流程：
 * 1. 遍历攻击方所有 BuffHook.onDamageDealt（如 Strength 增加、Weak 减少）
 * 2. 遍历防御方所有 BuffHook.onDamageReceived（如 Fragile 增加、Resistance 减少）
 * <p>
 * 引用方：DamageAction（执行伤害时调用）、CombatEngine（AOE 伤害计算）
 */
public final class DamageCalculator {

    private DamageCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 计算经过 Buff 修正后的最终伤害值。
     * <p>
     * 先调用攻击方的 onDamageDealt 修正伤害，
     * 再调用防御方的 onDamageReceived 修正伤害。
     * 最低伤害为 0。
     *
     * @param baseDamage 基础伤害值
     * @param attacker   攻击方实体
     * @param defender   防御方实体
     * @return 修正后的最终伤害值
     */
    public static int calculateDamage(int baseDamage,
                                      AbstractEntity attacker,
                                      AbstractEntity defender) {
        if (baseDamage <= 0) return 0;

        int damage = baseDamage;

        // 1. 遍历攻击方 Buff: onDamageDealt
        for (BuffHook buff : attacker.getBuffs()) {
            damage = buff.onDamageDealt(damage, defender);
        }

        // 2. 遍历防御方 Buff: onDamageReceived
        for (BuffHook buff : defender.getBuffs()) {
            damage = buff.onDamageReceived(damage, attacker);
        }

        return Math.max(0, damage);
    }

    /**
     * 计算经过 Buff 修正后的格挡值。
     * <p>
     * 遍历目标身上的 BuffHook.onBlockGained 进行修正。
     *
     * @param baseBlock 基础格挡获得量
     * @param entity    获得格挡的实体
     * @return 修正后的格挡值
     */
    public static int calculateBlock(int baseBlock, AbstractEntity entity) {
        int block = Math.max(0, baseBlock);
        for (BuffHook buff : entity.getBuffs()) {
            block = buff.onBlockGained(block);
        }
        return Math.max(0, block);
    }
}
