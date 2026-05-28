package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * OnEntityDeath — 实体死亡事件
 * <p>
 * 当某个实体（敌人或玩家）生命值降为 0 时发布。
 * 持有已死亡实体的引用和击杀者信息。
 * 用于触发战斗结束判定、藏品效果（如探灵伯爵）等。
 * <p>
 * 引用方：EventBus（发布/订阅）、CombatEngine（实体死亡时发布）、
 *         各藏品实现、BattleEndHandler
 */
public class OnEntityDeath extends GameEvent {

    /** 已死亡的实体 */
    private final AbstractEntity deceased;

    /** 击杀者实体（若存在） */
    private final AbstractEntity killer;

    /**
     * 构造实体死亡事件。
     *
     * @param sourceEntityId 触发事件的实体 ID
     * @param deceased       已死亡的实体
     * @param killer         击杀者实体（可为 null，如环境伤害导致的死亡）
     */
    public OnEntityDeath(String sourceEntityId, AbstractEntity deceased,
                         AbstractEntity killer) {
        super(sourceEntityId);
        this.deceased = deceased;
        this.killer = killer;
    }

    /**
     * 获取已死亡的实体。
     *
     * @return 死亡实体
     */
    public AbstractEntity getDeceased() {
        return deceased;
    }

    /**
     * 获取击杀者实体。
     *
     * @return 击杀者（可能为 null）
     */
    public AbstractEntity getKiller() {
        return killer;
    }

    /**
     * 判断是否为玩家死亡。
     *
     * @return true 如果死亡实体是玩家
     */
    public boolean isPlayerDeath() {
        return false; // 由子类或外部逻辑判断
    }
}
