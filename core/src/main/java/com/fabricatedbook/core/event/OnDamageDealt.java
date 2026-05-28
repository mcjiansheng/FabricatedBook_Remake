package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * OnDamageDealt — 造成伤害事件
 * <p>
 * 当某个实体（玩家或敌人）对另一个实体造成伤害时发布。
 * 持有伤害数值、伤害来源和伤害目标。
 * 藏品系统可通过订阅该事件实现"吸血""额外伤害"等效果。
 * <p>
 * 引用方：EventBus（发布/订阅）、DamageCalculator（发布）、
 *         各藏品实现（如 VampireFang 订阅该事件回复生命值）
 */
public class OnDamageDealt extends GameEvent {

    /** 最终伤害值（已过 Buff 修正） */
    private final int damage;

    /** 伤害来源实体 */
    private final AbstractEntity source;

    /** 伤害目标实体 */
    private final AbstractEntity target;

    /**
     * 构造造成伤害事件。
     *
     * @param sourceEntityId 触发事件的实体 ID
     * @param damage         最终造成的伤害值
     * @param source         伤害来源实体
     * @param target         伤害目标实体
     */
    public OnDamageDealt(String sourceEntityId, int damage,
                         AbstractEntity source, AbstractEntity target) {
        super(sourceEntityId);
        this.damage = damage;
        this.source = source;
        this.target = target;
    }

    /**
     * 获取最终伤害值（已过 Buff 修正）。
     *
     * @return 伤害值
     */
    public int getDamage() {
        return damage;
    }

    /**
     * 获取伤害来源实体。
     *
     * @return 来源实体
     */
    public AbstractEntity getSource() {
        return source;
    }

    /**
     * 获取伤害目标实体。
     *
     * @return 目标实体
     */
    public AbstractEntity getTarget() {
        return target;
    }
}
