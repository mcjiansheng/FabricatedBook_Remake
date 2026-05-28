package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * BuffHook — Buff 生命周期钩子接口（钩子模式）
 * <p>
 * 所有 Buff 效果通过实现该接口嵌入战斗结算流程。
 * 钩子方法有默认实现（空方法），子类只需重写需要参与结算的环节。
 * 核心结算方法 DamageCalculator 会在计算伤害时遍历目标的所有 BuffHook。
 * <p>
 * 引用方：AbstractEntity（持有 List<BuffHook>）、DamageCalculator（遍历调用）、各 Buff 实现类
 */
public interface BuffHook {

    /**
     * 受到伤害时触发 — 用于修正传入的原始伤害值。
     * <p>
     * 在 DamageCalculator 计算最终伤害时被调用。
     * 多个 Buff 的修正效果会叠加（如脆弱 +25% + 抗性 -25% 可部分抵消）。
     * <p>
     * 典型用例：
     * - Fragile（脆弱）：return (int)(damage * 1.25);
     * - Resistance（抗性）：return (int)(damage * 0.75);
     *
     * @param damage 进入此钩子前的原始伤害值（已叠加之前的 Buff 修正）
     * @param source 伤害来源实体
     * @return 修正后的伤害值，传递给下一个 BuffHook 或作为最终伤害
     */
    default int onDamageReceived(int damage, AbstractEntity source) {
        return damage;
    }

    /**
     * 造成伤害时触发 — 用于修正即将造成的伤害值。
     * <p>
     * 与 onDamageReceived 的区别：此方法在攻击方发出攻击时调用，
     * 用于修正攻击方的输出而非防御方的承受量。
     * <p>
     * 典型用例：
     * - Weak（虚弱）：return (int)(damage * 0.75);
     * - Strength（力量）：return (int)(damage * 1.25);
     *
     * @param damage 进入此钩子前的原始伤害值
     * @param target 伤害目标实体
     * @return 修正后的伤害值
     */
    default int onDamageDealt(int damage, AbstractEntity target) {
        return damage;
    }

    /**
     * 获得格挡时触发 — 用于修正格挡获得量。
     * <p>
     * 典型用例：
     * - BlockIncrease（坚强）：return (int)(block * 1.5);
     * - BlockReduction（易碎）：return (int)(block * 0.5);
     *
     * @param block 原始格挡获得量
     * @return 修正后的格挡值
     */
    default int onBlockGained(int block) {
        return block;
    }

    /**
     * 回合开始时触发 — 处理持续性效果。
     * <p>
     * 在每回合开始时被 CombatEngine 调用，用于处理中毒伤害、凋零结算等。
     * <p>
     * 典型用例：
     * - Poison（中毒）：对 owner 造成 poisonStack 点伤害，层数减 1
     * - Withering（凋零）：层数递增准备下回合引爆
     *
     * @param owner 该 Buff 所属的实体
     */
    default void onTurnStart(AbstractEntity owner) {
    }

    /**
     * 回合结束时触发 — 处理回合末结算。
     * <p>
     * 典型用例：
     * - ArmorBuff（装甲）：不清除格挡值
     * - 某些持续性效果的层数衰减
     *
     * @param owner 该 Buff 所属的实体
     */
    default void onTurnEnd(AbstractEntity owner) {
    }

    /**
     * 获取该 Buff 的名称。
     *
     * @return Buff 名称字符串
     */
    String getBuffName();

    /**
     * 获取该 Buff 的当前层数/堆叠数。
     *
     * @return 层数（0 表示该 Buff 已到期）
     */
    int getStack();
}
