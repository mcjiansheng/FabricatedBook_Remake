package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * ExtraEnergyBuff — 额外能量 Buff
 * <p>
 * 每回合额外获得能量。
 * 游戏百科中描述：每回合额外获得能量。
 * <p>
 * 正面效果。由卡牌"搏命挣扎"（每回合 +2 能量）或
 * 藏品"古怪的长笛"（战斗开始时获得 1 能量）等提供。
 * 引用方：CombatEngine（每回合开始时应用额外能量）。
 */
public class ExtraEnergyBuff extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "ExtraEnergyBuff";

    /** 每回合额外获得的能量数 */
    private final int extraEnergyPerTurn;

    /**
     * 构造额外能量 Buff。
     *
     * @param stack             层数（持续回合数，-1 表示持续整场战斗）
     * @param extraEnergyPerTurn 每回合额外能量数
     */
    public ExtraEnergyBuff(int stack, int extraEnergyPerTurn) {
        super(BUFF_NAME, Math.max(-1, stack));
        this.extraEnergyPerTurn = Math.max(0, extraEnergyPerTurn);
    }

    /**
     * 构造默认持续整场战斗的额外能量 Buff。
     *
     * @param extraEnergyPerTurn 每回合额外能量数
     */
    public ExtraEnergyBuff(int extraEnergyPerTurn) {
        this(-1, extraEnergyPerTurn);
    }

    /**
     * 获取每回合额外获得的能量数。
     *
     * @return 额外能量值
     */
    public int getExtraEnergyPerTurn() {
        return extraEnergyPerTurn;
    }

    @Override
    public void onTurnStart(AbstractEntity owner) {
        if (owner != null && owner.isAlive() && extraEnergyPerTurn > 0) {
            owner.gainEnergy(extraEnergyPerTurn);
        }

        // 如果不是无限持续，减少层数
        if (stack > 0) {
            stack--;
        }
    }
}
