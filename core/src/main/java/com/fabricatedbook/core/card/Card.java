package com.fabricatedbook.core.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Card — 卡牌数据载体
 * <p>
 * 代表游戏中的一张卡牌，包含所有基本属性。
 * 卡牌数据可由 Json 文件或硬编码创建。
 * 卡牌效果通过 effects 列表中的字符串描述，由 CombatEngine 解析生成对应 Action。
 * <p>
 * 引用方：CardPool（卡牌池）、CardFactory（创建）、Player/AbstractEntity（手牌/牌堆）、
 *         CombatEngine（解析效果）、OnCardUsed（事件参数）
 */
public class Card {

    /** 卡牌类型枚举 */
    public enum CardType {
        /** 🗡️ 攻击 — 直接造成伤害 */
        ATTACK,
        /** 🛡️ 防御 — 获得格挡值 */
        DEFENSE,
        /** ⚡ 技能 — 特殊效果 */
        SKILL,
        /** 🔧 装备 — 提供持续战斗效果 */
        EQUIP
    }

    /** 目标选择类型枚举 */
    public enum TargetType {
        /** 🎯 单个敌人 */
        SINGLE_ENEMY,
        /** 🎯 单个敌人（兼容用） */
        ENEMY,
        /** 👥 所有敌人 */
        ALL_ENEMIES,
        /** 🧑 自身（增益/回血等） */
        SELF,
        /** 🛡️ 所有友方 */
        ALL_ALLIES
    }

    /** 稀有度枚举 */
    public enum Rarity {
        /** ⚪ 基础（初始卡牌） */
        BASIC,
        /** 🔵 普通 */
        COMMON,
        /** 🟢 罕见 */
        UNCOMMON,
        /** 🟣 稀有 */
        RARE,
        /** 🟡 史诗 */
        EPIC,
        /** 🔴 传说 */
        LEGENDARY
    }

    // ====== 基本属性 ======

    /** 卡牌名称 */
    private String name;

    /** 卡牌消耗（能量） */
    private int cost;

    /** 卡牌效果描述 */
    private String description;

    /** 卡牌类型 */
    private CardType type;

    /** 稀有度 */
    private Rarity rarity;

    /** 价值（用于商店定价） */
    private int value;

    /** 目标选择类型 */
    private TargetType targetType;

    /** 目标数量（SINGLE_ENEMY 时为 1，ALL_ENEMIES 时表示敌人数） */
    private int targetCount;

    /** 效果列表 — 字符串标识，由 CombatEngine 解析 */
    private List<String> effects;

    /** 是否消耗（使用后消失，不进入弃牌堆） */
    private boolean exhaust;

    /** 所属职业（"warrior"/"mage"/"witch"/null=通用） */
    private String profession;

    /** 唯一标识符 */
    private String id;

    /**
     * 默认构造卡牌（通过 CardFactory 使用 Builder 模式创建）。
     */
    public Card() {
        this.effects = new ArrayList<>();
        this.exhaust = false;
        this.targetCount = 1;
        this.rarity = Rarity.COMMON;
    }

    /**
     * 全参构造卡牌。
     */
    public Card(String id, String name, int cost, String description,
                CardType type, Rarity rarity, int value,
                TargetType targetType, int targetCount,
                List<String> effects, boolean exhaust, String profession) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.description = description;
        this.type = type;
        this.rarity = rarity;
        this.value = value;
        this.targetType = targetType;
        this.targetCount = targetCount;
        this.effects = effects != null ? effects : new ArrayList<>();
        this.exhaust = exhaust;
        this.profession = profession;
    }

    // ====== Getter / Setter ======

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CardType getType() { return type; }
    public void setType(CardType type) { this.type = type; }

    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public List<String> getEffects() { return effects; }
    public void setEffects(List<String> effects) { this.effects = effects; }

    public boolean isExhaust() { return exhaust; }
    public void setExhaust(boolean exhaust) { this.exhaust = exhaust; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    // ====== 工具方法 ======

    /**
     * 判断是否为攻击牌。
     *
     * @return true 如果是攻击类型
     */
    public boolean isAttack() {
        return type == CardType.ATTACK;
    }

    /**
     * 判断是否为技能牌。
     *
     * @return true 如果是技能类型
     */
    public boolean isSkill() {
        return type == CardType.SKILL;
    }

    /**
     * 判断是否为防御牌。
     *
     * @return true 如果是防御类型
     */
    public boolean isDefense() {
        return type == CardType.DEFENSE;
    }

    /**
     * 判断是否为装备牌（消耗型持续效果）。
     *
     * @return true 如果是装备类型
     */
    public boolean isEquip() {
        return type == CardType.EQUIP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[" + cost + "]" + name + " - " + description;
    }
}
