package com.fabricatedbook.core.action;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * GainBlockAction — 获得格挡动作
 * <p>
 * 使目标实体获得格挡值。
 * 格挡获得量受目标身上的 Buff 影响（如坚强增加 50%，易碎减少 50%）。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class GainBlockAction implements CombatAction {

    /** 目标实体 */
    private final AbstractEntity target;

    /** 基础格挡获得量 */
    private final int baseBlock;

    /** 执行完成标志 */
    private boolean finished;

    /** 实际格挡获得量（Buff 修正后） */
    private int actualBlock;

    /**
     * 构造获得格挡动作。
     *
     * @param target    目标实体
     * @param baseBlock 基础格挡获得量
     */
    public GainBlockAction(AbstractEntity target, int baseBlock) {
        this.target = target;
        this.baseBlock = Math.max(0, baseBlock);
        this.finished = false;
        this.actualBlock = 0;
    }

    @Override
    public void execute() {
        if (target == null || !target.isAlive()) {
            finished = true;
            return;
        }

        // 遍历目标身上的 BuffHook 进行格挡值修正
        int modifiedBlock = baseBlock;
        for (BuffHook buff : target.getBuffs()) {
            modifiedBlock = buff.onBlockGained(modifiedBlock);
        }

        this.actualBlock = Math.max(0, modifiedBlock);
        target.gainBlock(actualBlock);
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return "获得 " + baseBlock + " 点格挡" +
                (actualBlock != baseBlock ? "（修正后 " + actualBlock + "）" : "");
    }

    // ====== Getter ======

    public AbstractEntity getTarget() { return target; }
    public int getBaseBlock() { return baseBlock; }
    public int getActualBlock() { return actualBlock; }
}
