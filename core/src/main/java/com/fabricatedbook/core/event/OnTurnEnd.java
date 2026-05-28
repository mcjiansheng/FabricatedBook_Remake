package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * OnTurnEnd — 回合结束事件
 * <p>
 * 当某实体的回合结束时发布。
 * 持有回合结束的实体引用，用于触发回合结束结算（如清除回合末效果）。
 * <p>
 * 引用方：EventBus（发布/订阅）、CombatEngine（回合结束时发布）、
 *         ArmorBuff（订阅该事件防止格挡被清除）
 */
public class OnTurnEnd extends GameEvent {

    /** 回合结束的实体 */
    private final AbstractEntity entity;

    /**
     * 构造回合结束事件。
     *
     * @param sourceEntityId 触发事件的实体 ID
     * @param entity         回合结束的实体
     */
    public OnTurnEnd(String sourceEntityId, AbstractEntity entity) {
        super(sourceEntityId);
        this.entity = entity;
    }

    /**
     * 获取回合结束的实体。
     *
     * @return 实体对象
     */
    public AbstractEntity getEntity() {
        return entity;
    }
}
