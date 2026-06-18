package com.fabricatedbook.core.action;

import com.fabricatedbook.core.engine.DamageCalculator;
import com.fabricatedbook.core.entity.AbstractEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DamageAction — 伤害动作
 * <p>
 * 对一个或多个目标造成伤害。
 * 执行时通过 DamageCalculator 计算经过 Buff 修正后的最终伤害值。
 * 是战斗中最常用的动作类型。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class DamageAction implements CombatAction {

    /** 伤害来源实体 */
    private final AbstractEntity source;

    /** 伤害目标列表 */
    private final List<AbstractEntity> targets;

    /** 基础伤害值（未经 Buff 修正） */
    private final int baseDamage;

    /** 是否为多段伤害的一段 */
    private final boolean isMultiHit;

    /** 执行完成标志 */
    private boolean finished;

    /** 每个目标实际承受的最终伤害（已计算 Buff，未扣格挡前的攻击数值）。 */
    private final Map<AbstractEntity, Integer> finalDamageByTarget;

    private DamageModifier damageModifier;

    @FunctionalInterface
    public interface DamageModifier {
        int modify(int damage, AbstractEntity source, AbstractEntity target);
    }

    /**
     * 构造伤害动作。
     *
     * @param source     伤害来源
     * @param targets    伤害目标列表
     * @param baseDamage 基础伤害值
     */
    public DamageAction(AbstractEntity source, List<AbstractEntity> targets,
                        int baseDamage) {
        this(source, targets, baseDamage, false);
    }

    /**
     * 构造伤害动作，支持多段标记。
     *
     * @param source      伤害来源
     * @param targets     伤害目标列表
     * @param baseDamage  基础伤害值
     * @param isMultiHit  是否为多段攻击的一段
     */
    public DamageAction(AbstractEntity source, List<AbstractEntity> targets,
                        int baseDamage, boolean isMultiHit) {
        this.source = source;
        this.targets = targets;
        this.baseDamage = Math.max(0, baseDamage);
        this.isMultiHit = isMultiHit;
        this.finished = false;
        this.finalDamageByTarget = new HashMap<>();
    }

    @Override
    public void execute() {
        finalDamageByTarget.clear();
        for (AbstractEntity target : targets) {
            if (target == null || !target.isAlive()) continue;

            // 通过 DamageCalculator 进行 Buff 修正
            int finalDamage = DamageCalculator.calculateDamage(baseDamage, source, target);
            if (damageModifier != null) {
                finalDamage = damageModifier.modify(finalDamage, source, target);
            }
            finalDamageByTarget.put(target, finalDamage);

            // 扣除格挡和生命值
            target.takeDamage(finalDamage);

            // 如果目标死亡，执行死亡判定
            if (!target.isAlive()) {
                // CombatEngine 会监听 OnEntityDeath
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
        if (targets.size() == 1) {
            return "对 " + targets.get(0).getName() + " 造成 " + baseDamage + " 点伤害";
        } else {
            return "对所有敌人造成 " + baseDamage + " 点伤害";
        }
    }

    // ====== Getter ======

    public AbstractEntity getSource() { return source; }
    public List<AbstractEntity> getTargets() { return targets; }
    public int getBaseDamage() { return baseDamage; }
    public boolean isMultiHit() { return isMultiHit; }
    public Map<AbstractEntity, Integer> getFinalDamageByTarget() { return finalDamageByTarget; }

    public DamageAction withAddedBaseDamage(int amount) {
        return new DamageAction(source, targets, baseDamage + Math.max(0, amount), isMultiHit);
    }

    public void setDamageModifier(DamageModifier damageModifier) {
        this.damageModifier = damageModifier;
    }
}
