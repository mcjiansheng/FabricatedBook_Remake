package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * BlockIncrease — 坚强 / 格挡增加 Buff
 * <p>
 * 获得的格挡值增加 50%（onBlockGained * 1.5）。
 * 游戏百科中描述：获得格挡 +50%。
 * <p>
 * 正面效果，与易碎(BlockReduction)效果部分抵消。
 * 引用方：卡牌"不屈"、猎人、老猎人、魔王等施加此 Buff。
 */
public class BlockIncrease extends AbstractBuff {

    /** Buff 唯一名称 */
    private static final String BUFF_NAME = "BlockIncrease";

    /**
     * 构造坚强 Buff。
     *
     * @param stack 层数（回合数）
     */
    public BlockIncrease(int stack) {
        super(BUFF_NAME, stack);
    }

    @Override
    public int onBlockGained(int block) {
        return (int) Math.ceil(block * 1.5);
    }

    @Override
    public void onTurnEnd(AbstractEntity owner) {
        // 回合结束层数减 1
        stack--;
    }
}
