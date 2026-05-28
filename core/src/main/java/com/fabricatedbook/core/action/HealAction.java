package com.fabricatedbook.core.action;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * HealAction — 回血动作
 * <p>
 * 对目标实体进行生命值回复。
 * 回血量受治疗效果增益藏品影响（如洗手液）。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class HealAction implements CombatAction {

    /** 回血目标 */
    private final AbstractEntity target;

    /** 原始回血量 */
    private final int baseHeal;

    /** 执行完成标志 */
    private boolean finished;

    /** 实际回血记录 */
    private int actualHeal;

    /**
     * 构造回血动作。
     *
     * @param target   回血目标
     * @param baseHeal 原始回血量
     */
    public HealAction(AbstractEntity target, int baseHeal) {
        this.target = target;
        this.baseHeal = Math.max(0, baseHeal);
        this.finished = false;
        this.actualHeal = 0;
    }

    @Override
    public void execute() {
        if (target == null || !target.isAlive()) {
            finished = true;
            return;
        }

        // 计算回血量（未来可在此处应用治疗效果增益）
        int healAmount = baseHeal;

        // 执行回血
        this.actualHeal = target.heal(healAmount);
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return target.getName() + " 回复 " + baseHeal + " 点生命值";
    }

    // ====== Getter ======

    public AbstractEntity getTarget() { return target; }
    public int getBaseHeal() { return baseHeal; }
    public int getActualHeal() { return actualHeal; }
}
