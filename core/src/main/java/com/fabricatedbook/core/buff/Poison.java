package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Poison — 中毒 Buff
 * <p>
 * 中毒结算由 CombatEngine 在阵营指定时机显式触发。
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

    public int tick(AbstractEntity owner, boolean blockedByBlock) {
        if (stack <= 0) return 0;
        if (owner == null || !owner.isAlive()) return 0;

        int damage = owner.modifyStatusDamage(stack, BUFF_NAME);
        int hpLoss;
        if (blockedByBlock) {
            hpLoss = owner.takeDamage(damage);
        } else {
            int oldHp = owner.getHp();
            owner.setHp(owner.getHp() - damage);
            hpLoss = oldHp - owner.getHp();
        }

        stack--;
        return hpLoss;
    }
}
