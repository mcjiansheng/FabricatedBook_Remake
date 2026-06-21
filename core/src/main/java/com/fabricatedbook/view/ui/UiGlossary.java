package com.fabricatedbook.view.ui;

import com.fabricatedbook.core.card.Card;

/**
 * Front-end wording for the terms already defined in the game encyclopedia.
 * This class deliberately contains no calculation or rule ownership: it only
 * gives compact hover copy to terms that are otherwise too dense for card art.
 */
public final class UiGlossary {

    private UiGlossary() {}

    public static String cardDetails(Card card) {
        if (card == null) return "";
        StringBuilder details = new StringBuilder();
        append(details, typeDetail(card.getType()));
        String description = card.getDescription();
        if (description != null) {
            appendIfMentioned(details, description, "消耗", "使用后进入消耗牌堆，不进入弃牌堆；消耗牌堆不参与本场战斗洗牌。");
            appendIfMentioned(details, description, "保留", "回合结束时仍留在手牌中，不进入弃牌堆。");
            appendIfMentioned(details, description, "虚无", "回合结束时若仍在手牌中，则进入消耗牌堆。");
            appendIfMentioned(details, description, "脆弱", "受到伤害增加 25%。");
            appendIfMentioned(details, description, "易碎", "获得的格挡 -50%。");
            appendIfMentioned(details, description, "虚弱", "造成的伤害减少 25%。");
            appendIfMentioned(details, description, "眩晕", "下一回合无法行动。");
            appendIfMentioned(details, description, "中毒", "按阵营在指定时机结算层数伤害，层数 -1。");
            appendIfMentioned(details, description, "凋零", "回合开始或受特定技能引爆，每次引爆伤害增加。");
            appendIfMentioned(details, description, "抗性", "受到伤害减少 25%。");
            appendIfMentioned(details, description, "坚强", "获得格挡 +50%。");
            appendIfMentioned(details, description, "力量", "造成的伤害增加 25%。");
            appendIfMentioned(details, description, "装甲", "格挡值不再在回合结束时消失。");
            appendIfMentioned(details, description, "不死", "血量降为 0 也不会死亡。");
            appendIfMentioned(details, description, "额外能量", "每回合额外获得能量。");
        }
        if (card.getCost() < 0) append(details, "X 费：打出时消耗当前所有剩余能量，消耗量记为 X。");
        return details.toString();
    }

    public static String potionDetails(String name, String description) {
        if (name == null || name.isBlank()) return description == null ? "" : description;
        return name + (description == null || description.isBlank() ? "" : "\n" + description);
    }

    private static String typeDetail(Card.CardType type) {
        if (type == null) return "";
        return switch (type) {
            case ATTACK -> "攻击：直接造成伤害。";
            case DEFENSE, SKILL -> "技能：提供格挡、治疗、抽牌、临时 Buff/Debuff 或一次性特殊效果。";
            case ABILITY -> "能力：提供持续规则改变或长期战斗效果；打出后不会进入弃牌堆，也不被视为消耗。";
            case STATUS -> "状态：通常由战斗或敌人生成，污染牌组或限制行动。";
            case CURSE -> "诅咒：通常为负面牌，进入牌组后带来持续代价。";
            case TASK -> "任务：用于特殊目标、剧情或路线机制。";
            case EQUIP -> "";
        };
    }

    private static void appendIfMentioned(StringBuilder details, String source, String keyword, String meaning) {
        if (source.contains(keyword)) append(details, keyword + "：" + meaning);
    }

    private static void append(StringBuilder details, String text) {
        if (text == null || text.isBlank()) return;
        if (details.length() > 0) details.append('\n');
        details.append(text);
    }
}
