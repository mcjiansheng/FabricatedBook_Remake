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
        /** ✨ 能力 — 打出后离场，不进入弃牌堆或消耗堆 */
        ABILITY,
        /** 🔧 装备 — 提供持续战斗效果 */
        EQUIP,
        /** ⚠️ 状态 — 通常由战斗生成 */
        STATUS,
        /** 💀 诅咒 — 通常为负面牌 */
        CURSE,
        /** 📜 任务 — 特殊目标或剧情牌 */
        TASK
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

    /** 是否消耗（使用后进入消耗牌堆，不进入弃牌堆） */
    private boolean exhaust;

    /** 是否保留（回合结束留在手牌中）。 */
    private boolean retain;

    /** 是否虚无（回合结束仍在手牌中则进入消耗牌堆）。 */
    private boolean ethereal;

    /** 是否无法被打出。 */
    private boolean unplayable;

    /** 是否已经升级。 */
    private boolean upgraded;

    /** 升级后的消耗；为 null 表示不改变。 */
    private Integer upgradedCost;

    /** 升级后的描述；为 null 表示不改变。 */
    private String upgradedDescription;

    /** 升级后的效果；为 null 表示不改变。 */
    private List<String> upgradedEffects;

    /** 升级后的消耗牌标记；为 null 表示不改变。 */
    private Boolean upgradedExhaust;

    /** 升级后的保留标记；为 null 表示不改变。 */
    private Boolean upgradedRetain;

    /** 升级后的虚无标记；为 null 表示不改变。 */
    private Boolean upgradedEthereal;

    /** 升级后的无法打出标记；为 null 表示不改变。 */
    private Boolean upgradedUnplayable;

    /** 所属职业（"warrior"/"mage"/"witch"/null=通用） */
    private String profession;

    /** 扩散伤害累计加成（不参与 JSON 序列化，仅战斗中用于 escalating 效果） */
    private transient int escalatingBonus = 0;

    /** 唯一标识符 */
    private String id;

    /**
     * 默认构造卡牌（通过 CardFactory 使用 Builder 模式创建）。
     */
    public Card() {
        this.effects = new ArrayList<>();
        this.exhaust = false;
        this.retain = false;
        this.ethereal = false;
        this.unplayable = false;
        this.upgraded = false;
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

    public Card(String id, String name, int cost, String description,
                CardType type, Rarity rarity, int value,
                TargetType targetType, int targetCount,
                List<String> effects, boolean exhaust, boolean retain,
                boolean ethereal, String profession) {
        this(id, name, cost, description, type, rarity, value, targetType,
                targetCount, effects, exhaust, profession);
        this.retain = retain;
        this.ethereal = ethereal;
    }

    public Card(String id, String name, int cost, String description,
                CardType type, Rarity rarity, int value,
                TargetType targetType, int targetCount,
                List<String> effects, boolean exhaust, boolean retain,
                boolean ethereal, boolean unplayable, String profession) {
        this(id, name, cost, description, type, rarity, value, targetType,
                targetCount, effects, exhaust, retain, ethereal, profession);
        this.unplayable = unplayable;
    }

    // ====== Getter / Setter ======

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return upgraded ? name + "+" : name; }
    public String getBaseName() { return name; }
    public void setName(String name) {
        if (name != null && name.endsWith("+")) {
            this.name = name.substring(0, name.length() - 1);
            this.upgraded = true;
        } else {
            this.name = name;
        }
    }

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

    public boolean isRetain() { return retain; }
    public void setRetain(boolean retain) { this.retain = retain; }

    public boolean isEthereal() { return ethereal; }
    public void setEthereal(boolean ethereal) { this.ethereal = ethereal; }

    public boolean isUnplayable() { return unplayable; }
    public void setUnplayable(boolean unplayable) { this.unplayable = unplayable; }

    public boolean isUpgraded() { return upgraded; }
    public void setUpgraded(boolean upgraded) { this.upgraded = upgraded; }

    public boolean canUpgrade() {
        return !upgraded
                && type != CardType.STATUS
                && type != CardType.CURSE
                && type != CardType.TASK
                && (upgradedCost != null
                || upgradedDescription != null
                || upgradedEffects != null
                || upgradedExhaust != null
                || upgradedRetain != null
                || upgradedEthereal != null
                || upgradedUnplayable != null);
    }

    public boolean upgrade() {
        if (!canUpgrade()) return false;
        if (upgradedCost != null) cost = upgradedCost;
        if (upgradedDescription != null) description = upgradedDescription;
        if (upgradedEffects != null) effects = new ArrayList<>(upgradedEffects);
        if (upgradedExhaust != null) exhaust = upgradedExhaust;
        if (upgradedRetain != null) retain = upgradedRetain;
        if (upgradedEthereal != null) ethereal = upgradedEthereal;
        if (upgradedUnplayable != null) unplayable = upgradedUnplayable;
        upgraded = true;
        return true;
    }

    public void setUpgrade(Integer cost, String description, List<String> effects,
                           Boolean exhaust, Boolean retain, Boolean ethereal,
                           Boolean unplayable) {
        this.upgradedCost = cost;
        this.upgradedDescription = description;
        this.upgradedEffects = effects != null ? new ArrayList<>(effects) : null;
        this.upgradedExhaust = exhaust;
        this.upgradedRetain = retain;
        this.upgradedEthereal = ethereal;
        this.upgradedUnplayable = unplayable;
    }

    public Integer getUpgradedCost() { return upgradedCost; }
    public String getUpgradedDescription() { return upgradedDescription; }
    public List<String> getUpgradedEffects() {
        return upgradedEffects != null ? new ArrayList<>(upgradedEffects) : null;
    }
    public Boolean getUpgradedExhaust() { return upgradedExhaust; }
    public Boolean getUpgradedRetain() { return upgradedRetain; }
    public Boolean getUpgradedEthereal() { return upgradedEthereal; }
    public Boolean getUpgradedUnplayable() { return upgradedUnplayable; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    /** 扩散伤害累计加成（用于 escalating 效果，不参与 JSON 序列化） */
    public int getEscalatingBonus() { return escalatingBonus; }
    public void addEscalatingBonus(int bonus) { this.escalatingBonus += bonus; }

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

    public boolean isAbility() {
        return type == CardType.ABILITY;
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
        return "[" + cost + "]" + getName() + " - " + description;
    }
}
