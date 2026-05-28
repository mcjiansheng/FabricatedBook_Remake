package com.fabricatedbook.core.action;

/**
 * CombatAction — 战斗动作接口（命令模式）
 * <p>
 * 所有战斗中的可执行动作（伤害、回血、抽牌、施加Buff等）都实现该接口。
 * 每个动作封装为一个独立的命令对象，由 ActionManager 排队执行。
 * View 层通过 isFinished() 判断动作的动画是否播放完毕，以协调前后端节奏。
 * <p>
 * 引用方：CombatEngine（生成动作）、ActionManager（执行动作）、ViewNotifier（通知动画）
 */
public interface CombatAction {

    /**
     * 执行具体的数值修改逻辑。
     * 此方法被 ActionManager 调用，执行期间会遍历目标的 BuffHook 进行数值修正。
     * 例如：DamageAction.execute() 会在造成伤害前检查目标是否有脆弱(Fragile)Buff。
     */
    void execute();

    /**
     * 判断该动作是否已执行完成（包括动画播放完毕）。
     * <p>
     * 用于 View 层轮询：当 isFinished() 返回 true 时，
     * ViewNotifier 才会通知前端播放下一个动作的动画。
     *
     * @return true 表示动作已完全结束，可以执行下一个动作
     */
    boolean isFinished();

    /**
     * 获取该动作的文本描述，用于前端显示（如："造成 12 点伤害"）。
     *
     * @return 动作描述字符串
     */
    String getDescription();
}
