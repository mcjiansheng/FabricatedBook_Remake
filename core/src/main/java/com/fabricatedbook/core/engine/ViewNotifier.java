package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.entity.AbstractEntity;

import java.util.List;

/**
 * ViewNotifier — 视图通知接口（观察者模式）
 * <p>
 * 战斗引擎在关键生命周期节点调用此接口的方法，
 * View 层实现此接口以同步播放动画和更新 UI。
 * <p>
 * 引用方：CombatEngine（战斗生命周期中调用）、
 *         View 层（BattleScreen 等实现此接口）
 */
public interface ViewNotifier {

    /**
     * 战斗开始时调用。
     *
     * @param player  玩家实体
     * @param enemies 敌人列表
     */
    void onBattleStart(AbstractEntity player, List<AbstractEntity> enemies);

    /**
     * 每个动作执行完后调用，用于播放下一个动画。
     *
     * @param action 刚刚执行完成的动作
     */
    void onActionExecuted(CombatAction action);

    /**
     * 战斗结束时调用（无论胜利或失败）。
     *
     * @param victory true 表示玩家胜利
     * @param reward  胜利时的奖励描述（可为空）
     */
    void onBattleEnd(boolean victory, String reward);

    /**
     * 每回合开始时调用。
     *
     * @param turnNumber 当前回合数（从 1 开始）
     */
    void onTurnStart(int turnNumber);

    /**
     * 玩家使用卡牌时调用。
     *
     * @param action 卡牌生成的战斗动作
     */
    void onCardPlayed(CombatAction action);
}
