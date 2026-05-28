package com.fabricatedbook.core.event;

import com.fabricatedbook.core.card.Card;

/**
 * OnCardUsed — 使用卡牌事件
 * <p>
 * 当玩家使用一张卡牌时发布。
 * 持有被使用的 Card 对象引用，便于藏品系统根据卡牌信息触发效果。
 * <p>
 * 引用方：EventBus（发布/订阅）、CombatEngine（使用卡牌时发布）、
 *         各藏品实现（如吸血鬼的尖牙订阅该事件回复生命值）
 */
public class OnCardUsed extends GameEvent {

    /** 被使用的卡牌 */
    private final Card card;

    /** 使用该卡牌消耗的能量 */
    private final int energyCost;

    /**
     * 构造使用卡牌事件。
     *
     * @param sourceEntityId 触发事件的实体 ID（通常为玩家 ID）
     * @param card           被使用的卡牌
     * @param energyCost     消耗的能量值
     */
    public OnCardUsed(String sourceEntityId, Card card, int energyCost) {
        super(sourceEntityId);
        this.card = card;
        this.energyCost = energyCost;
    }

    /**
     * 获取被使用的卡牌。
     *
     * @return 卡牌对象
     */
    public Card getCard() {
        return card;
    }

    /**
     * 获取该卡牌消耗的能量值。
     *
     * @return 能量消耗
     */
    public int getEnergyCost() {
        return energyCost;
    }
}
