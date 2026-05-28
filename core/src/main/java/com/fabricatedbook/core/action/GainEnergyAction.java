package com.fabricatedbook.core.action;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * GainEnergyAction — 获得能量动作
 * <p>
 * 为目标实体增加能量值。
 * 常用于卡牌效果（如"英雄登场"获得 3 点能量）。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class GainEnergyAction implements CombatAction {

    /** 目标实体 */
    private final AbstractEntity target;

    /** 能量获得量 */
    private final int amount;

    /** 执行完成标志 */
    private boolean finished;

    /**
     * 构造获得能量动作。
     *
     * @param target 目标实体（通常为玩家）
     * @param amount 能量增加量
     */
    public GainEnergyAction(AbstractEntity target, int amount) {
        this.target = target;
        this.amount = Math.max(0, amount);
        this.finished = false;
    }

    @Override
    public void execute() {
        if (target == null || !target.isAlive()) {
            finished = true;
            return;
        }
        target.gainEnergy(amount);
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return "获得 " + amount + " 点能量";
    }

    // ====== Getter ======

    public AbstractEntity getTarget() { return target; }
    public int getAmount() { return amount; }
}
