package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Strength — 力量 Buff
 * <p>
 * 造成的伤害增加 25%（onDamageDealt * 1.25）。
 * 游戏百科中描述：造成的伤害增加 25%。
 * <p>
 * 正面效果，与虚弱(Weak)效果部分抵消。
 * 引用方：卡牌"沸血"、猎人、拾荒者、哥布林等施加此 Buff。
 */
public class Strength extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Strength";

    /**
     * 构造力量 Buff。
     *
     * @param stack 层数（回合数）
     */
    public Strength(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onDamageDealt(int damage, AbstractEntity target) {
        return (int) Math.ceil(damage * 1.25);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        stack--;
    }
}
