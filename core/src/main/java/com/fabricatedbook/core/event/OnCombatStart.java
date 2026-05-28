package com.fabricatedbook.core.event;

/**
 * OnCombatStart — 战斗开始事件
 * <p>
 * 当一场战斗开始时发布。
 * 藏品系统可通过订阅该事件在战斗开始时获得能量、抽牌等效果。
 * <p>
 * 引用方：EventBus（发布/订阅）、CombatEngine（战斗开始时发布）、
 *         各藏品实现（如微缩舞台模型、古怪的长笛）
 */
public class OnCombatStart extends GameEvent {

    /** 战斗等级编号（1-5，对应五层地图） */
    private final int level;

    /**
     * 构造战斗开始事件。
     *
     * @param sourceEntityId 触发事件的实体 ID（通常为玩家 ID）
     * @param level          战斗层级
     */
    public OnCombatStart(String sourceEntityId, int level) {
        super(sourceEntityId);
        this.level = level;
    }

    /**
     * 获取当前战斗的层级。
     *
     * @return 层级编号（1-5）
     */
    public int getLevel() {
        return level;
    }
}
