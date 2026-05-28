package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * ArmorBuff — 装甲 Buff
 * <p>
 * 格挡值不再在回合结束时消失。
 * 游戏百科中描述：格挡值不再在回合结束时消失。
 * <p>
 * 正面效果。通过订阅 OnTurnEnd 事件在回合结束时阻止格挡清除。
 * 通常由卡牌"装甲"（Armorer）施加。
 * 引用方：CombatEngine（回合结束时检查装甲 Buff 决定是否清除格挡）。
 */
public class ArmorBuff extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "ArmorBuff";

    /**
     * 构造装甲 Buff。
     * <p>
     * 装甲通常为无持续时间 Buff（持续整场战斗）。
     *
     * @param stack 层数（通常为 1，表示生效）
     */
    public ArmorBuff(int stack) {
        super(BUFF_NAME, stack);
    }

    /**
     * 判断是否应保留格挡。
     * 装甲存在时在回合结束时不清除格挡。
     *
     * @return true 如果装甲生效
     */
    public boolean shouldPreserveBlock() {
        return stack > 0;
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 装甲本身不消耗层数，持续生效
        // 如果装甲 Buff 被移除（如"兴奋"清除负面效果），
        // 格挡将在下一回合结束时正常清除
    }
}
