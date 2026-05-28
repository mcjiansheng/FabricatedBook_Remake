package com.fabricatedbook.core.relic;

import com.fabricatedbook.core.event.*;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;

import java.util.*;

/**
 * RelicManager — 藏品管理器
 * <p>
 * 管理玩家已获得的藏品列表。
 * 战斗开始时自动订阅藏品的所有事件效果，
 * 战斗结束后取消订阅。
 * 提供统一的事件接口供其他系统调用。
 * <p>
 * 引用方：CombatEngine（战斗开始/结束时调用）、
 *         Player（持有已获得的藏品）、
 *         ShopManager（购买藏品时调用）
 */
public class RelicManager {

    /** 玩家引用 */
    private final Player player;

    /** 事件总线引用 */
    private final EventBus bus;

    /** 当前战斗是否进行中 */
    private boolean inCombat;

    /**
     * 构造藏品管理器。
     *
     * @param player 玩家实体
     */
    public RelicManager(Player player) {
        this.player = player;
        this.bus = EventBus.getInstance();
        this.inCombat = false;
    }

    /**
     * 获得一个新藏品。
     *
     * @param relic 藏品实例
     */
    public void addRelic(Relic relic) {
        if (relic == null) return;
        player.addRelic(relic);
        System.out.println("[RelicManager] 获得藏品: " + relic.getName());
    }

    /**
     * 移除一个藏品（仅用于测试或特殊事件）。
     *
     * @param relicName 藏品名称
     */
    public void removeRelic(String relicName) {
        player.getRelics().removeIf(r -> r.getName().equals(relicName));
    }

    /**
     * 战斗开始时 — 为所有已拥有藏品注册事件订阅。
     */
    public void onCombatStart() {
        if (inCombat) return;
        inCombat = true;

        for (Relic relic : player.getRelics()) {
            relic.subscribe(bus);
        }

        // 发布战斗开始事件
        bus.publish(new OnCombatStart(player.getId(),
                player.getCurrentFloor()));
    }

    /**
     * 战斗结束时 — 取消所有藏品的事件订阅。
     */
    public void onCombatEnd() {
        if (!inCombat) return;

        for (Relic relic : player.getRelics()) {
            relic.unsubscribe(bus);
        }

        inCombat = false;
    }

    /**
     * 发布伤害事件。
     *
     * @param damage 伤害值
     * @param source 伤害来源
     * @param target 伤害目标
     */
    public void fireDamageDealt(int damage, AbstractEntity source,
                                 AbstractEntity target) {
        if (!inCombat) return;
        bus.publish(new OnDamageDealt(player.getId(), damage, source, target));
    }

    /**
     * 发布使用卡牌事件。
     *
     * @param effect 描述（简化版）
     */
    public void fireCardUsed(String effect) {
        if (!inCombat) return;
        bus.publish(new OnCardUsed(player.getId(), null, 0));
    }

    /**
     * 发布回合结束事件。
     *
     * @param entity 回合结束的实体
     */
    public void fireTurnEnd(AbstractEntity entity) {
        if (!inCombat) return;
        bus.publish(new OnTurnEnd(player.getId(), entity));
    }

    /**
     * 发布实体死亡事件。
     *
     * @param deceased 已死亡的实体
     * @param killer   击杀者
     */
    public void fireEntityDeath(AbstractEntity deceased, AbstractEntity killer) {
        if (!inCombat) return;
        bus.publish(new OnEntityDeath(player.getId(), deceased, killer));
    }

    /**
     * 检查是否持有指定名称的藏品。
     *
     * @param relicName 藏品名称
     * @return true 如果已持有
     */
    public boolean hasRelic(String relicName) {
        return player.hasRelic(relicName);
    }

    /**
     * 获取所有已拥有的藏品。
     *
     * @return 藏品列表
     */
    public List<Relic> getOwnedRelics() {
        return Collections.unmodifiableList(player.getRelics());
    }
}
