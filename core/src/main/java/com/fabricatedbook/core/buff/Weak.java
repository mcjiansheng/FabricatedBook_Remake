package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Weak — 虚弱 Buff
 * <p>
 * 造成的伤害减少 25%（onDamageDealt * 0.75）。
 * 游戏百科中描述：造成的伤害减少 25%。
 * <p>
 * 负面效果，与力量(Strength)效果部分抵消。
 * 引用方：卡牌"奇袭"、盗贼、老猎人、幻影等施加此 Buff。
 */
public class Weak extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Weak";

    /**
     * 构造虚弱 Buff。
     *
     * @param stack 层数（回合数）
     */
    public Weak(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onDamageDealt(int damage, AbstractEntity target) {
        return (int) Math.floor(damage * 0.75);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        stack--;
    }
}
