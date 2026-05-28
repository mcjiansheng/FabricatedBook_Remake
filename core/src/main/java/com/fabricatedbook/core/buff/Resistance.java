package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Resistance — 抗性 Buff
 * <p>
 * 受到伤害减少 25%（onDamageReceived * 0.75）。
 * 游戏百科中描述：受到伤害减少 25%。
 * <p>
 * 正面效果，与脆弱(Fragile)效果部分抵消。
 * 引用方：孤魂野鬼、卫士统领、幕后黑手傀儡等施加此 Buff。
 */
public class Resistance extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Resistance";

    /**
     * 构造抗性 Buff。
     *
     * @param stack 层数（回合数）
     */
    public Resistance(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onDamageReceived(int damage, AbstractEntity source) {
        return (int) Math.floor(damage * 0.75);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 回合结束层数减 1
        stack--;
    }
}
