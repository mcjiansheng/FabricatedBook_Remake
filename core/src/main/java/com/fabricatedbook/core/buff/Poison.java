package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Poison — 中毒 Buff
 * <p>
 * 回合开始时造成相当于层数的伤害，然后层数减 1。
 * 游戏百科中描述：回合开始时依层数受到伤害，层数 -1。
 * <p>
 * 负面效果。对拥有者造成持续伤害，每回合递减。
 * 引用方：卡牌"淬毒""瘟疫"、盗贼、哥布林、毒刃等施加此 Buff。
 */
public class Poison extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Poison";

    /**
     * 构造中毒 Buff。
     *
     * @param stack 层数（中毒伤害量）
     */
    public Poison(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public void onTurnStart(AbstractEntity owner) {
        if (stack <= 0) return;
        if (owner == null || !owner.isAlive()) return;

        // Poison is direct HP loss and must not consume block.
        int damage = owner.modifyStatusDamage(stack, BUFF_NAME);
        owner.setHp(owner.getHp() - damage);

        // 层数减 1
        stack--;
    }
}
