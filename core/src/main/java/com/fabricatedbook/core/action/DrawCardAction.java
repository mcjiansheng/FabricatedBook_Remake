package com.fabricatedbook.core.action;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * DrawCardAction — 抽牌动作
 * <p>
 * 使目标实体从抽牌堆抽取指定数量的牌进入手牌。
 * 如果抽牌堆不足，会自动将弃牌堆洗回抽牌堆。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class DrawCardAction implements CombatAction {

    /** 目标实体 */
    private final AbstractEntity target;

    /** 抽牌数量 */
    private final int count;

    /** 执行完成标志 */
    private boolean finished;

    /** 实际抽到的牌数 */
    private int actualDrawn;

    /**
     * 构造抽牌动作。
     *
     * @param target 目标实体（通常为玩家）
     * @param count  抽牌数量
     */
    public DrawCardAction(AbstractEntity target, int count) {
        this.target = target;
        this.count = Math.max(0, count);
        this.finished = false;
        this.actualDrawn = 0;
    }

    @Override
    public void execute() {
        if (target == null || !target.isAlive()) {
            finished = true;
            return;
        }
        this.actualDrawn = target.drawCards(count);
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return "抽取 " + count + " 张牌" +
                (actualDrawn != count ? "（实际抽到 " + actualDrawn + " 张）" : "");
    }

    // ====== Getter ======

    public AbstractEntity getTarget() { return target; }
    public int getCount() { return count; }
    public int getActualDrawn() { return actualDrawn; }
}
