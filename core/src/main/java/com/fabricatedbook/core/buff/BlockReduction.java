package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * BlockReduction — 易碎 / 格挡减少 Buff
 * <p>
 * 获得的格挡值减少 50%（onBlockGained * 0.5）。
 * 游戏百科中描述：获得的格挡 -50%。
 * <p>
 * 负面效果，与坚强(BlockIncrease)效果部分抵消。
 * 引用方：卡牌"痛击""英雄登场""弱点"等施加此 Buff。
 */
public class BlockReduction extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "BlockReduction";

    /**
     * 构造易碎 Buff。
     *
     * @param stack 层数（回合数）
     */
    public BlockReduction(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onBlockGained(int block) {
        return (int) Math.floor(block * 0.5);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 回合结束层数减 1
        stack--;
    }
}
