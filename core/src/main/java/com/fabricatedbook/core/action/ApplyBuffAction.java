package com.fabricatedbook.core.action;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.AbstractEntity;

import java.util.List;

/**
 * ApplyBuffAction — 施加 Buff 动作
 * <p>
 * 对目标实体施加指定 Buff 效果。
 * 支持对单个或多个目标施加相同 Buff。
 * 如果目标已持有同名 Buff，通常增加层数而非替换。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class ApplyBuffAction implements CombatAction {

    /** 目标列表 */
    private final List<AbstractEntity> targets;

    /** Buff 工厂方法（用于生成新的 BuffHook 实例） */
    private final BuffFactory buffFactory;

    /** Buff 层数 */
    private final int stacks;

    /** 执行完成标志 */
    private boolean finished;

    /**
     * ApplyBuffAction — Buff 工厂函数式接口
     * <p>
     * 用于延迟创建 BuffHook 实例。
     */
    @FunctionalInterface
    public interface BuffFactory {
        /**
         * 创建指定层数的 Buff 实例。
         *
         * @param stacks 层数
         * @return BuffHook 实例
         */
        BuffHook create(int stacks);
    }

    /**
     * 构造施加 Buff 动作。
     *
     * @param targets     目标列表
     * @param buffFactory Buff 创建工厂
     * @param stacks      Buff 层数
     */
    public ApplyBuffAction(List<AbstractEntity> targets, BuffFactory buffFactory,
                           int stacks) {
        this.targets = targets;
        this.buffFactory = buffFactory;
        this.stacks = Math.max(1, stacks);
        this.finished = false;
    }

    /**
     * 构造对单个目标的施加 Buff 动作。
     *
     * @param target      单个目标
     * @param buffFactory Buff 创建工厂
     * @param stacks      Buff 层数
     */
    public ApplyBuffAction(AbstractEntity target, BuffFactory buffFactory,
                           int stacks) {
        this(List.of(target), buffFactory, stacks);
    }

    /**
     * 通过 Buff 名称和层数构造施加 Buff 动作（便捷构造器）。
     * <p>
     * 内部通过 BuffFactory 的 lambda 调用 BuffResolver 解析 Buff 名称。
     *
     * @param target   目标实体
     * @param buffName Buff 名称（如 "Fragile", "Strength", "Poison" 等）
     * @param stacks   Buff 层数
     */
    public ApplyBuffAction(AbstractEntity target, String buffName, int stacks) {
        this(List.of(target), BuffResolver.resolve(buffName), stacks);
    }

    /**
     * 通过 Buff 名称和层数构造施加 Buff 动作（便捷构造器，多目标）。
     *
     * @param targets  目标列表
     * @param buffName Buff 名称
     * @param stacks   Buff 层数
     */
    public ApplyBuffAction(List<AbstractEntity> targets, String buffName, int stacks) {
        this(targets, BuffResolver.resolve(buffName), stacks);
    }

    @Override
    public void execute() {
        for (AbstractEntity target : targets) {
            if (target == null || !target.isAlive()) continue;

            // 检查是否已有同名 Buff，有则增加层数
            BuffHook existing = null;
            for (BuffHook buff : target.getBuffs()) {
                if (buff.getBuffName().equals(buffFactory.create(1).getBuffName())) {
                    existing = buff;
                    break;
                }
            }

            if (existing != null) {
                // 已有相同 Buff，增加层数（实际层数更新由 Buff 实现自身控制）
                // 此处需要特殊处理：通过反射或额外接口更新
                // 简单处理：移除旧 Buff 并添加新 Buff（层数叠加）
                target.removeBuff(existing.getBuffName());
                int totalStacks = existing.getStack() + stacks;
                target.addBuff(buffFactory.create(totalStacks));
            } else {
                target.addBuff(buffFactory.create(stacks));
            }
        }
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        String buffName = buffFactory.create(1).getBuffName();
        return "为目标施加 " + stacks + " 层 " + buffName;
    }

    // ====== Getter ======

    public List<AbstractEntity> getTargets() { return targets; }
    public int getStacks() { return stacks; }
}
