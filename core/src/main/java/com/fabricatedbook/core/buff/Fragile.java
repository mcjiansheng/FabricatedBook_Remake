package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Fragile — 脆弱 / 易伤 Buff
 * <p>
 * 受到伤害增加 25%（onDamageReceived * 1.25）。
 * 游戏百科中描述：受到伤害增加 25%。
 * <p>
 * 负面效果，与抗性(Resistance)效果部分抵消。
 * 引用方：卡牌"痛击""哈撒给""弱点""英雄登场"等施加此 Buff。
 */
public class Fragile extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Fragile";

    /**
     * 构造脆弱 Buff。
     *
     * @param stack 层数（回合数）
     */
    public Fragile(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onDamageReceived(int damage, AbstractEntity source) {
        return (int) Math.ceil(damage * 1.25);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 回合结束层数减 1
        stack--;
    }
}
