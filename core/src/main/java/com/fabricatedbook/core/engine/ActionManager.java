package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.CombatAction;

import java.util.LinkedList;
import java.util.Queue;

/**
 * ActionManager — 战斗动作队列管理器（命令队列模式）
 * <p>
 * 管理 CombatAction 的执行队列。
 * 战斗引擎(push)入动作后，逐条调用 executeNext 执行。
 * View 层通过 isAllFinished 判断队列是否清空，以协调动画播放。
 * <p>
 * 引用方：CombatEngine（入队和执行动作）、ViewNotifier（通知前端动画）
 */
public class ActionManager {

    /** 动作执行队列 */
    private final Queue<CombatAction> actionQueue;

    /** 当前正在执行的动作 */
    private CombatAction currentAction;

    /**
     * 构造动作管理器。
     */
    public ActionManager() {
        this.actionQueue = new LinkedList<>();
        this.currentAction = null;
    }

    /**
     * 将一个动作加入队列尾部。
     *
     * @param action 要入队的战斗动作
     */
    public void push(CombatAction action) {
        if (action != null) {
            actionQueue.offer(action);
        }
    }

    /**
     * 执行队列中的下一个动作。
     * <p>
     * 如果当前有动作正在执行且未完成，不执行新动作。
     * 从队列头部取出动作并调用其 execute() 方法。
     *
     * @return 正在执行的动作，如果队列为空返回 null
     */
    public CombatAction executeNext() {
        // 如果当前动作还在执行中（动画未播放完），不继续
        if (currentAction != null && !currentAction.isFinished()) {
            return currentAction;
        }

        // 从队列取出下一个动作
        currentAction = actionQueue.poll();
        if (currentAction != null) {
            currentAction.execute();
        }
        return currentAction;
    }

    /**
     * 判断队列是否为空且当前动作已结束。
     *
     * @return true 如果所有动作都已执行完毕
     */
    public boolean isAllFinished() {
        return actionQueue.isEmpty() && (currentAction == null || currentAction.isFinished());
    }

    /**
     * 执行队列中所有动作直到全部完成。
     * <p>
     * 在非动画模式下（后端测试或快速结算）直接执行完所有动作。
     */
    public void executeAll() {
        while (!isAllFinished()) {
            if (currentAction == null || currentAction.isFinished()) {
                executeNext();
            } else {
                // 理论上 executeAll 时不需要等待动画
                break;
            }
        }
    }

    /**
     * 获取当前正在执行的动作。
     *
     * @return 当前动作，无动作时返回 null
     */
    public CombatAction getCurrentAction() {
        return currentAction;
    }

    /**
     * 获取队列中剩余的未执行动作数。
     *
     * @return 队列长度
     */
    public int getRemainingCount() {
        return actionQueue.size();
    }

    /**
     * 清空动作队列（用于战斗中断或重新开始）。
     */
    public void clear() {
        actionQueue.clear();
        currentAction = null;
    }
}
