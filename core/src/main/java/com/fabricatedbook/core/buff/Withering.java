package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Withering — 凋零 Buff
 * <p>
 * 回合开始阶层数递增（准备引爆）。
 * 可通过 detonate() 方法引爆，造成递增伤害。
 * 游戏百科中描述：回合开始或受特定技能引爆，每次引爆伤害增加。
 * <p>
 * 负面效果。凋零是游戏中最复杂的持续伤害机制：
 * - 每回合层数递增（标记累积）
 * - 引爆时造成当前层数的伤害
 * - 引爆后层数重置为 0（下次从 1 开始）
 * <p>
 * 引用方：卡牌"腐化""瘟疫"、孤魂野鬼、凋零之树、幕后黑手等。
 */
public class Withering extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Withering";

    /** 累计引爆次数（用于计算伤害递增系数） */
    private int detonationCount;

    /**
     * 构造凋零 Buff。
     *
     * @param stack 初始层数
     */
    public Withering(int stack) {
        super(BUFF_NAME, stack);
        this.detonationCount = 0;
    }

    @Override
    public void onTurnStart(AbstractEntity owner) {
        // 回合开始层数递增（标记累积）
        if (owner != null && owner.isAlive()) {
            stack++;
        }
    }

    /**
     * 引爆凋零 — 造成当前层数伤害并重置层数。
     * <p>
     * 每次引爆后层数归零，下次从新累积。
     * detonationCount 用于逐次增加引爆伤害基础值，
     * 游戏百科描述"每次引爆伤害增加"意味着引爆基础值递增。
     *
     * @param owner 受伤害的实体
     * @return 造成的实际伤害值
     */
    public int detonate(AbstractEntity owner) {
        if (owner == null || !owner.isAlive() || stack <= 0) {
            return 0;
        }

        // 引爆伤害 = 当前层数 + 引爆次数（逐次递增）
        int damage = stack + detonationCount;

        // 直接对生命值造成伤害（穿透格挡）
        owner.setHp(owner.getHp() - damage);

        // 记录引爆并重置
        detonationCount++;
        stack = 0;

        return damage;
    }

    /**
     * 获取当前累计引爆次数。
     *
     * @return 引爆次数
     */
    public int getDetonationCount() {
        return detonationCount;
    }

    /**
     * 获取当前凋零层数（不触发引爆）。
     *
     * @return 当前层数
     */
    public int getWitheringStack() {
        return stack;
    }
}
