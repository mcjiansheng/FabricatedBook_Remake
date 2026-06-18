package com.fabricatedbook.core.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * CardFactory — 卡牌工厂
 * <p>
 * 从 JSON 数据文件或硬编码参数生成 Card 实例。
 * 支持从文件系统路径和类路径加载 JSON。
 * <p>
 * 引用方：DataLoader（加载卡牌数据）、CardPool（注册卡牌）、
 *         CombatEngine（生成卡牌效果对应的 Action）
 */
public class CardFactory {

    /**
     * 从卡牌池获取卡牌副本（深拷贝浅层属性，便于修改）。
     *
     * @param template 模板卡牌
     * @return 新的 Card 实例（同名但独立对象）
     */
    public static Card createFromTemplate(Card template) {
        if (template == null) return null;
        return new Card(
                template.getId(), template.getName(), template.getCost(),
                template.getDescription(), template.getType(), template.getRarity(),
                template.getValue(), template.getTargetType(), template.getTargetCount(),
                new ArrayList<>(template.getEffects()), template.isExhaust(),
                template.isRetain(), template.isEthereal(),
                template.isUnplayable(),
                template.getProfession()
        );
    }

    /**
     * 从 JSON 文件路径加载卡牌列表。
     *
     * @param filePath JSON 文件路径（类路径或文件系统路径）
     * @return 卡牌列表
     */
    public static List<Card> createFromJson(String filePath) {
        List<Card> cards = new ArrayList<>();
        try {
            JsonArray jsonArray;
            // 尝试从类路径加载
            InputStream is = CardFactory.class.getResourceAsStream(filePath);
            if (is != null) {
                jsonArray = JsonParser.parseReader(new InputStreamReader(is))
                        .getAsJsonArray();
            } else {
                jsonArray = JsonParser.parseReader(new FileReader(filePath))
                        .getAsJsonArray();
            }

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                Card card = new Card();

                card.setId(getJsonString(obj, "id"));
                card.setName(getJsonString(obj, "name"));
                card.setCost(getJsonInt(obj, "cost", 0));
                card.setDescription(getJsonString(obj, "description"));
                card.setType(Card.CardType.valueOf(
                        getJsonString(obj, "type", "ATTACK")));
                card.setRarity(Card.Rarity.valueOf(
                        getJsonString(obj, "rarity", "COMMON")));
                card.setValue(getJsonInt(obj, "value", 0));
                card.setTargetType(Card.TargetType.valueOf(
                        getJsonString(obj, "targetType", "SINGLE_ENEMY")));
                card.setTargetCount(getJsonInt(obj, "targetCount", 1));
                card.setExhaust(getJsonBool(obj, "exhaust", false));
                card.setRetain(getJsonBool(obj, "retain", false));
                card.setEthereal(getJsonBool(obj, "ethereal", false));
                card.setUnplayable(getJsonBool(obj, "unplayable", false));
                card.setProfession(getJsonString(obj, "profession", null));

                if (obj.has("effects")) {
                    List<String> effects = new ArrayList<>();
                    for (JsonElement e : obj.get("effects").getAsJsonArray()) {
                        effects.add(e.getAsString());
                    }
                    card.setEffects(effects);
                }

                cards.add(card);
            }
        } catch (Exception e) {
            System.err.println("CardFactory: 加载卡牌 JSON 失败: " + filePath);
            e.printStackTrace();
        }
        return cards;
    }

    /**
     * 创建卡牌效果对应的 Action 列表描述（由 CombatEngine 解释）。
     * <p>
     * 效果字符串格式约定：
     * - "damage:N" — 对单目标造成 N 点伤害
     * - "damage_all:N" — 对所有敌人造成 N 点伤害
     * - "damage:N:M" — 对单目标造成 N 点伤害，M 段
     * - "damage_all:N:M" — 对所有敌人造成 N 点伤害，M 段
     * - "block:N" — 获得 N 点格挡
     * - "heal:N" — 回复 N 点生命值
     * - "draw:N" — 抽 N 张牌
     * - "energy:N" — 获得 N 点能量
     * - "debuff:NAME:STACK" — 对目标施加 STACK 层 NAME Debuff
     * - "debuff_all:NAME:STACK" — 对所有敌人施加 Debuff
     * - "buff:self:NAME:STACK" — 对自己施加 Buff
     * - "purify" — 清除所有负面效果
     * - ...
     *
     * @param card 要解析的卡牌
     * @return 效果字符串列表
     */
    public static List<String> resolveEffects(Card card) {
        return card.getEffects();
    }

    // ====== JSON 解析工具 ======

    private static String getJsonString(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "";
    }

    private static String getJsonString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    private static int getJsonInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static boolean getJsonBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }
}
