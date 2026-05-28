package com.fabricatedbook.core.action;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * RemoveCardAction — 移除卡牌动作
 * <p>
 * 从目标实体（通常为玩家）的指定位置移除一张卡牌。
 * 可用于从手牌、抽牌堆或弃牌堆移除卡牌（如"祈祷"事件移除一张牌）。
 * <p>
 * 引用方：CombatEngine（生成动作）、CombatAction（接口实现）
 */
public class RemoveCardAction implements CombatAction {

    /** 目标实体 */
    private final AbstractEntity target;

    /** 要移除的卡牌索引（在手牌中的索引，-1 表示移除最右侧的牌） */
    private final int cardIndex;

    /** 移除位置标识（"hand"/"draw"/"discard"） */
    private final String fromPile;

    /** 直接指定要移除的卡牌（优先级高于索引） */
    private final Card specificCard;

    /** 执行完成标志 */
    private boolean finished;

    /** 被移除的卡牌（存档用） */
    private Card removedCard;

    /**
     * 按索引从手牌移除卡牌。
     *
     * @param target    目标实体
     * @param cardIndex 手牌索引（-1 表示最右侧的牌）
     */
    public RemoveCardAction(AbstractEntity target, int cardIndex) {
        this.target = target;
        this.cardIndex = cardIndex;
        this.fromPile = "hand";
        this.specificCard = null;
        this.finished = false;
    }

    /**
     * 按索引从指定位置移除卡牌。
     *
     * @param target    目标实体
     * @param cardIndex 卡牌索引
     * @param fromPile  位置标识（"hand"/"draw"/"discard"）
     */
    public RemoveCardAction(AbstractEntity target, int cardIndex, String fromPile) {
        this.target = target;
        this.cardIndex = cardIndex;
        this.fromPile = fromPile;
        this.specificCard = null;
        this.finished = false;
    }

    /**
     * 直接移除指定卡牌实例。
     *
     * @param target 目标实体
     * @param card   要移除的卡牌
     */
    public RemoveCardAction(AbstractEntity target, Card card) {
        this.target = target;
        this.cardIndex = -1;
        this.fromPile = "hand";
        this.specificCard = card;
        this.finished = false;
    }

    @Override
    public void execute() {
        if (target == null) {
            finished = true;
            return;
        }

        if (specificCard != null) {
            // 按卡牌实例移除
            if (target.getHand().remove(specificCard)) {
                removedCard = specificCard;
            } else if (target.getDrawPile().remove(specificCard)) {
                removedCard = specificCard;
            } else if (target.getDiscardPile().remove(specificCard)) {
                removedCard = specificCard;
            }
        } else {
            // 按索引移除
            int idx = cardIndex < 0
                    ? target.getHand().size() - 1
                    : Math.min(cardIndex, target.getHand().size() - 1);

            if (idx >= 0 && idx < target.getHand().size()) {
                Card card;
                switch (fromPile) {
                    case "draw":
                        if (idx < target.getDrawPile().size()) {
                            card = target.getDrawPile().remove(idx);
                            removedCard = card;
                        }
                        break;
                    case "discard":
                        if (idx < target.getDiscardPile().size()) {
                            card = target.getDiscardPile().remove(idx);
                            removedCard = card;
                        }
                        break;
                    case "hand":
                    default:
                        card = target.getHand().remove(idx);
                        removedCard = card;
                        break;
                }
            }
        }
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        if (removedCard != null) {
            return "移除卡牌：" + removedCard.getName();
        }
        return "移除一张卡牌";
    }

    // ====== Getter ======

    public AbstractEntity getTarget() { return target; }
    public Card getRemovedCard() { return removedCard; }
}
