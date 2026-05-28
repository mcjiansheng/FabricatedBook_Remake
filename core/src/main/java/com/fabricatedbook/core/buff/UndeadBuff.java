package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * UndeadBuff — 不死 Buff
 * <p>
 * 血量降为 0 也不会死亡。
 * 游戏百科中描述：血量降为 0 也不会死亡。
 * <p>
 * 正面效果。由卡牌"搏命挣扎"施加：
 * - 3 回合内血量降为 0 也不会死亡
 * - 每回合 +2 能量
 * - 3 回合后直接死亡
 * <p>
 * 引用方：CombatEngine（伤害判定时检查该 Buff 防止死亡）、
 *         卡牌"搏命挣扎"的生成逻辑。
 */
public class UndeadBuff extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "UndeadBuff";

    /** 剩余死亡倒计时回合数 */
    private int turnsRemaining;

    /**
     * 构造不死 Buff。
     *
     * @param totalTurns 总持续回合数（搏命挣扎为 3）
     */
    public UndeadBuff(int totalTurns) {
        super(BUFF_NAME, totalTurns);
        this.turnsRemaining = totalTurns;
    }

    /**
     * 检查当前是否处于不死状态。
     *
     * @return true 如果还在不死持续期内
     */
    public boolean isUndeadActive() {
        return stack > 0;
    }

    /**
     * 获取剩余的回合数。
     *
     * @return 剩余回合数
     */
    public int getTurnsRemaining() {
        return turnsRemaining;
    }

    /**
     * 减小不死回合计数。
     * 在回合结束时调用。当回合数归零时，标记实体死亡。
     *
     * @param owner 拥有该 Buff 的实体
     */
    public void tick(AbstractEntity owner) {
        if (owner == null || !owner.isAlive()) return;

        turnsRemaining--;
        stack = turnsRemaining;

        if (turnsRemaining <= 0) {
            // 不死结束，实体死亡
            owner.setAlive(false);
            owner.setHp(0);
        }
    }
}
