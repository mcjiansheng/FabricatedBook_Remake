package com.fabricatedbook.core.event;

/**
 * GameEvent — 游戏事件基类
 * <p>
 * 所有游戏事件的抽象基类，用于事件总线(EventBus)的订阅/发布机制。
 * 藏品系统通过监听具体的事件子类来触发效果。
 * <p>
 * 引用方：EventBus（事件分发）、各 GameEvent 子类、RelicManager（订阅事件）
 */
public abstract class GameEvent {

    /** 事件发生的时间戳（ms） */
    private final long timestamp;

    /** 事件的来源实体标识 */
    private final String sourceEntityId;

    /**
     * 构造一个游戏事件。
     *
     * @param sourceEntityId 触发该事件的实体 ID
     */
    protected GameEvent(String sourceEntityId) {
        this.timestamp = System.currentTimeMillis();
        this.sourceEntityId = sourceEntityId;
    }

    /**
     * 获取事件发生的时间戳。
     *
     * @return 毫秒级时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取触发该事件的实体 ID。
     *
     * @return 实体 ID 字符串
     */
    public String getSourceEntityId() {
        return sourceEntityId;
    }

    /**
     * 获取事件类型的唯一标识，用于 EventBus 的事件匹配。
     *
     * @return 事件类型字符串（通常为类名）
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
