package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Dizziness — 眩晕 Buff
 * <p>
 * 使目标下一回合无法行动。
 * 游戏百科中描述：下一回合无法行动。
 * <p>
 * 负面效果。当目标拥有该 Buff 时，回合开始时跳过行动。
 * 不需要通过 onDamageReceived/onDamageDealt 接口实现，
 * 而是由 CombatEngine 在每回合开始时检查目标是否被眩晕。
 * 引用方：卡牌"肘击"、幕后黑手等施加此 Buff。
 */
public class Dizziness extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "Dizziness";

    /**
     * 构造眩晕 Buff。
     *
     * @param stack 层数（眩晕回合数，通常为 1）
     */
    public Dizziness(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public void onTurnStart(AbstractEntity owner) {
        // 回合开始时标记实体为眩晕
        if (stack > 0) {
            owner.setDizzy(true);
            stack--;
        }
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 回合结束时清除眩晕标记
        if (stack <= 0) {
            owner.setDizzy(false);
        }
    }
}
