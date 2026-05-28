package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.event.GameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * EventBus — 事件总线（单例 + 发布订阅模式）
 * <p>
 * 游戏全局事件总线，用于解耦事件发布者和订阅者。
 * 藏品系统通过订阅特定事件来触发效果。
 * 支持按事件类型（Class）订阅/取消订阅/发布。
 * <p>
 * 引用方：RelicManager（为每个藏品注册事件订阅）、
 *         CombatEngine（在合适的时机发布事件）、
 *         DamageCalculator（发布 OnDamageDealt 事件）
 */
public class EventBus {

    /** 单例实例 */
    private static final EventBus INSTANCE = new EventBus();

    /** 事件订阅映射：事件类型 -> 订阅者列表 */
    private final Map<Class<? extends GameEvent>, List<Consumer<? extends GameEvent>>> subscribers;

    /** 是否启用事件日志 */
    private boolean loggingEnabled;

    /**
     * 私有构造函数（单例）。
     */
    private EventBus() {
        this.subscribers = new ConcurrentHashMap<>();
        this.loggingEnabled = false;
    }

    /**
     * 获取事件总线单例实例。
     *
     * @return EventBus 实例
     */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * 订阅指定类型的事件。
     *
     * @param <T>        事件类型泛型
     * @param eventClass 事件类
     * @param callback   事件触发时的回调函数
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void subscribe(Class<T> eventClass,
                                                 Consumer<T> callback) {
        subscribers.computeIfAbsent(eventClass, k -> new ArrayList<>())
                .add((Consumer<? extends GameEvent>) callback);
    }

    /**
     * 取消订阅指定事件类型的指定回调。
     *
     * @param <T>        事件类型泛型
     * @param eventClass 事件类
     * @param callback   要移除的回调函数
     */
    public <T extends GameEvent> void unsubscribe(Class<T> eventClass,
                                                   Consumer<T> callback) {
        List<Consumer<? extends GameEvent>> list = subscribers.get(eventClass);
        if (list != null) {
            list.remove(callback);
            if (list.isEmpty()) {
                subscribers.remove(eventClass);
            }
        }
    }

    /**
     * 取消订阅者对指定事件类型的所有回调。
     *
     * @param eventClass 事件类
     */
    public void unsubscribeAll(Class<? extends GameEvent> eventClass) {
        subscribers.remove(eventClass);
    }

    /**
     * 发布事件 — 通知所有订阅了该事件类型的回调。
     *
     * @param <T>   事件类型泛型
     * @param event 要发布的事件
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        Class<? extends GameEvent> eventClass = event.getClass();

        if (loggingEnabled) {
            System.out.println("[EventBus] 发布: " + event.getEventType()
                    + " 来自 " + event.getSourceEntityId());
        }

        List<Consumer<? extends GameEvent>> list = subscribers.get(eventClass);
        if (list != null) {
            // 遍历副本避免并发修改
            List<Consumer<? extends GameEvent>> snapshot = new ArrayList<>(list);
            for (Consumer<? extends GameEvent> callback : snapshot) {
                ((Consumer<T>) callback).accept(event);
            }
        }
    }

    /**
     * 清除所有订阅。
     */
    public void clear() {
        subscribers.clear();
    }

    /**
     * 获取当前已注册的事件类型数量。
     *
     * @return 事件类型数量
     */
    public int getRegisteredEventCount() {
        return subscribers.size();
    }

    /**
     * 开启或关闭事件日志。
     *
     * @param enabled true 启用日志
     */
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }
}
