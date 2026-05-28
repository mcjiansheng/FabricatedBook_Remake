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

        // 对拥有者造成中毒层数的伤害
        int damage = stack;
        owner.takeDamage(damage);

        // 实际扣除生命值（忽略格挡）
        // 注意：takeDamage 会先扣格挡再扣血，中毒应该穿透格挡
        // 直接扣生命值
        owner.setHp(owner.getHp() - damage);

        // 层数减 1
        stack--;
    }
}
