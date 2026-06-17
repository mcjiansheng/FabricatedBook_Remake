package com.fabricatedbook.core.card;

import com.fabricatedbook.core.entity.Profession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CardPool — 卡牌池
 * <p>
 * 按职业分组的卡牌池，提供按职业和稀有度的卡牌检索。
 * 战士卡牌在此硬编码。
 * 法师和女巫的卡牌池当前留空，由 JSON 配置补充。
 * <p>
 * 引用方：CardFactory（注册卡牌）、ShopManager（生成商店卡牌）、
 *         CombatEngine（战斗奖励等）
 */
public class CardPool {

    /** 卡牌池：职业 -> 卡牌列表 */
    private static final Map<String, List<Card>> poolByProfession = new HashMap<>();

    /** 所有卡牌（无职业过滤） */
    private static final List<Card> allCards = new ArrayList<>();

    /** 随机数生成器 */
    private static final Random RANDOM = new Random();

    // 静态初始化：注册战士卡牌
    static {
        registerWarriorCards();
    }

    /**
     * 注册战士卡牌。
     * <p>
     * 数据来源：game_encyclopedia.md "四、卡牌系统 - 战士卡牌池"
     */
    private static void registerWarriorCards() {
        String profession = "warrior";
        List<Card> warriorCards = new ArrayList<>();

        // 1. 攻击 (Attack) — 攻击 | 1 | 造成 6 点伤害 | 价值 0
        warriorCards.add(createCard("war_atk1", "攻击", 1,
                "造成 6 点伤害", Card.CardType.ATTACK, Card.Rarity.BASIC, 0,
                Card.TargetType.SINGLE_ENEMY, 1, List.of("damage:6"), false, profession));

        // 2. 防御 (Defense) — 防御 | 1 | 获得 6 点格挡 | 价值 0
        warriorCards.add(createCard("war_def1", "防御", 1,
                "获得 6 点格挡", Card.CardType.DEFENSE, Card.Rarity.BASIC, 0,
                Card.TargetType.SELF, 1, List.of("block:6"), false, profession));

        // 3. 痛击 (Painful Blow) — 攻击 | 2 | 造成 8 点伤害，提供 2 回合脆弱 | 价值 1
        warriorCards.add(createCard("war_painful_blow", "痛击", 2,
                "造成 8 伤害，2 回合脆弱", Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:8", "debuff:fragile:2"),
                false, profession));

        // 4. 横扫 (Sweep) — 攻击 | 1 | 对所有单位造成 5 点伤害 | 价值 1
        warriorCards.add(createCard("war_sweep", "横扫", 1,
                "对所有敌人造成 5 点伤害", Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:5"), false, profession));

        // 5. 奇袭 (Ambush) — 技能 | 0 | 造成 5 点伤害 + 3 回合虚弱，消耗 | 价值 1
        warriorCards.add(createCard("war_ambush", "奇袭", 0,
                "造成 5 点伤害 + 3 回合虚弱，消耗", Card.CardType.SKILL, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:5", "debuff:weak:3"), true, profession));

        // 6. 反击 (Counterattack) — 技能 | 1 | 根据格挡值造成伤害 | 价值 1
        warriorCards.add(createCard("war_counterattack", "反击", 1,
                "根据格挡值造成伤害", Card.CardType.SKILL, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("counter:block"), false, profession));

        // 7. 强力击 (Powerful Strike) — 攻击 | 2 | 造成 10 伤害，手牌每张攻击牌 +3 | 价值 1
        warriorCards.add(createCard("war_powerful_strike", "强力击", 2,
                "造成 10 点伤害，手牌每张攻击牌使伤害 +3", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:10", "bonus_per_attack:3"), false, profession));

        // 8. 装甲 (Armorer) — 装备 | 2 | 装甲效果，消耗 | 价值 2
        warriorCards.add(createCard("war_armorer", "装甲", 2,
                "格挡值不再在回合结束时消失，消耗", Card.CardType.EQUIP, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SELF, 1,
                List.of("buff:self:armor"), true, profession));

        // 9. 致命节奏 (Deadly Tempo) — 攻击 | 1 | 造成 7 伤害，每使用一次 +1 | 价值 1
        warriorCards.add(createCard("war_deadly_tempo", "致命节奏", 1,
                "造成 7 点伤害，每使用一次该牌伤害 +1", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:7", "escalating:1"), false, profession));

        // 10. 底牌 (ACE) — 技能 | 0 | 抽 5 张牌，消耗 | 价值 2
        warriorCards.add(createCard("war_ace", "底牌", 0,
                "抽 5 张牌，消耗", Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SELF, 1,
                List.of("draw:5"), true, profession));

        // 11. 搏命挣扎 (Struggle Desperately) — 技能 | 0 | 不死 3 回合，每回合 +2 能量，消耗 | 价值 2
        warriorCards.add(createCard("war_struggle", "搏命挣扎", 0,
                "3 回合内不死，每回合 +2 能量，3 回合后死亡，消耗",
                Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SELF, 1,
                List.of("buff:self:undead:3", "buff:self:extra_energy:3:2"), true, profession));

        // 12. 兴奋 (Excited) — 技能 | 1 | 清除所有负面效果，消耗 | 价值 2
        warriorCards.add(createCard("war_excited", "兴奋", 1,
                "清除所有负面效果，消耗", Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SELF, 1,
                List.of("purify"), true, profession));

        // 13. 治疗 (Heal) — 技能 | 1 | 回复 10 生命值，消耗 | 价值 1
        warriorCards.add(createCard("war_heal", "治疗", 1,
                "回复 10 生命值，消耗", Card.CardType.SKILL, Card.Rarity.COMMON, 1,
                Card.TargetType.SELF, 1,
                List.of("heal:10"), true, profession));

        // 14. 致命一击 (Critical Hit) — 攻击 | 3 | 造成 30 点伤害 | 价值 2
        warriorCards.add(createCard("war_critical_hit", "致命一击", 3,
                "造成 30 点伤害", Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:30"), false, profession));

        // 15. 沸血 (Boiling Blood) — 技能 | 0 | 获得 2 回合力量，随机一张攻击牌 | 价值 1
        warriorCards.add(createCard("war_boiling_blood", "沸血", 0,
                "获得 2 回合力量，随机一张攻击牌", Card.CardType.SKILL,
                Card.Rarity.COMMON, 1, Card.TargetType.SELF, 1,
                List.of("buff:self:strength:2", "add_random_attack"), false, profession));

        // 16. 肘击 (Elbowing) — 攻击 | 1 | 造成 6 伤害，25% 眩晕 | 价值 1
        warriorCards.add(createCard("war_elbowing", "肘击", 1,
                "造成 6 点伤害，25% 概率眩晕 1 回合", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:6", "stun_chance:25"), false, profession));

        // 17. 剑刃风暴 (Bladestorm) — 攻击 | 2 | 对所有敌人造成 3×3 伤害 | 价值 2
        warriorCards.add(createCard("war_bladestorm", "剑刃风暴", 2,
                "对所有敌人造成 3×3 点伤害", Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:3:3"), false, profession));

        // 18. 哈撒给 (Hasaki) — 攻击 | 0 | 造成 4 伤害，3 回合易碎 | 价值 1
        warriorCards.add(createCard("war_hasaki", "哈撒给", 0,
                "造成 4 点伤害，3 回合易碎", Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:4", "debuff:block_reduction:3"), false, profession));

        // 19. 龙牙 (Dragon Fang) — 攻击 | 1 | 造成 9 伤害，敌方 <30 生命则 +5 | 价值 1
        warriorCards.add(createCard("war_dragon_fang", "龙牙", 1,
                "造成 9 点伤害，敌方生命值 <30 则伤害 +5", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:9", "bonus_low_hp:30:5"), false, profession));

        // 20. 英雄登场 (Hero Entrance) — 技能 | 0 | 12 AOE + 2 脆弱易碎 + 3 能量，消耗 | 价值 2
        warriorCards.add(createCard("war_hero_entrance", "英雄登场", 0,
                "对所有敌人造成 12 伤害 + 2 回合脆弱和易碎，获得 3 能量，消耗",
                Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:12", "debuff_all:fragile:2",
                        "debuff_all:block_reduction:2", "energy:3"),
                true, profession));

        // 21. 封尘绝念斩 (Fate Sealed) — 攻击 | 2 | 对所有敌人造成 16 伤害 | 价值 2
        warriorCards.add(createCard("war_fate_sealed", "封尘绝念斩", 2,
                "对所有敌人造成 16 点伤害", Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:16"), false, profession));

        // 22. 腐化 (Corrupt) — 攻击 | 1 | 造成 4 凋零并引爆 1 次 | 价值 1
        warriorCards.add(createCard("war_corrupt", "腐化", 1,
                "造成 4 层凋零并引爆 1 次", Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("debuff:withering:4", "trigger_withering"), false, profession));

        // 23. 瘟疫 (Plague) — 技能 | 2 | 对全体 6 中毒，中毒层数翻倍，消耗 | 价值 2
        warriorCards.add(createCard("war_plague", "瘟疫", 2,
                "对所有敌人造成 6 点中毒，并使所有敌人中毒层数翻倍，消耗",
                Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("debuff_all:poison:6", "double_poison"),
                true, profession));

        // 24. 淬毒 (Poisoned) — 攻击 | 1 | 5×2 伤害，每次 50% 中毒 | 价值 2
        warriorCards.add(createCard("war_poisoned", "淬毒", 1,
                "造成 5×2 点伤害，每次 50% 概率附加中毒", Card.CardType.ATTACK,
                Card.Rarity.UNCOMMON, 2, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:5:2", "chance_debuff:Poison:1:50"), false, profession));

        // 25. 弱点 (Weakness) — 技能 | 0 | 1 回合脆弱和易碎 | 价值 1
        warriorCards.add(createCard("war_weakness", "弱点", 0,
                "造成 1 回合脆弱和易碎", Card.CardType.SKILL, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("debuff:fragile:1", "debuff:block_reduction:1"), false, profession));

        // 26. 歪瓜 (Cheat) — 技能 | 0 | 获得 30 格挡，消耗 | 价值 2
        warriorCards.add(createCard("war_cheat", "歪瓜", 0,
                "获得 30 点格挡，消耗", Card.CardType.SKILL, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SELF, 1,
                List.of("block:30"), true, profession));

        // 27. 爆裂黎明 (Blast Dawn) — 技能 | 5 | 6×10 AOE，消耗 | 价值 3
        warriorCards.add(createCard("war_blast_dawn", "爆裂黎明", 5,
                "对所有敌人造成 6×10 点伤害，消耗", Card.CardType.SKILL,
                Card.Rarity.EPIC, 3, Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:6:10"), true, profession));

        // 28. 凛神斩 (Reckoning) — 攻击 | 1 | 对所有敌人 3 伤害，每目标提供 5 格挡 | 价值 1
        warriorCards.add(createCard("war_reckoning", "凛神斩", 1,
                "对所有敌人造成 3 点伤害，每个目标提供 5 格挡",
                Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.ALL_ENEMIES, 0,
                List.of("damage_all:3", "block_per_target:5"), false, profession));

        // 29. 肉斩骨断 (Flesh & Bone) — 攻击 | 1 | 5 伤害，每损失 10 生命 +3 | 价值 2
        warriorCards.add(createCard("war_flesh_bone", "肉斩骨断", 1,
                "造成 5 点伤害，每损失 10 生命值伤害 +3", Card.CardType.ATTACK,
                Card.Rarity.UNCOMMON, 2, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:5", "bonus_per_damage_taken:10:3"), false, profession));

        // 30. 不屈 (Unyielding) — 防御 | 2 | 获得 3 坚强，10 格挡 | 价值 1
        warriorCards.add(createCard("war_unyielding", "不屈", 2,
                "获得 3 回合坚强，10 点格挡", Card.CardType.DEFENSE, Card.Rarity.COMMON, 1,
                Card.TargetType.SELF, 1,
                List.of("block:10", "buff:self:block_increase:3"), false, profession));

        // 31. 根除 — 攻击 | X | 造成 X 段 11 点伤害，保留 | 价值 2
        warriorCards.add(createCard("war_eradicate", "根除", -1,
                "造成 X 段 11 点伤害，保留", Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage_x:11"), false, true, false, profession));

        // 32. 违逆 — 技能 | 1 | 获得 7 格挡，施加 2 层虚弱，虚无 | 价值 1
        warriorCards.add(createCard("war_defy", "违逆", 1,
                "获得 7 格挡，对一名敌人造成 2 层虚弱，虚无",
                Card.CardType.SKILL, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("block:7", "debuff:weak:2"), false, false, true, profession));

        poolByProfession.put(profession, warriorCards);
        allCards.addAll(warriorCards);
    }

    /**
     * 创建一个卡牌实例的便捷方法。
     */
    private static Card createCard(String id, String name, int cost,
                                   String description, Card.CardType type,
                                   Card.Rarity rarity, int value,
                                   Card.TargetType targetType, int targetCount,
                                   List<String> effects, boolean exhaust,
                                   String profession) {
        return new Card(id, name, cost, description, type, rarity, value,
                targetType, targetCount, effects, exhaust, profession);
    }

    private static Card createCard(String id, String name, int cost,
                                   String description, Card.CardType type,
                                   Card.Rarity rarity, int value,
                                   Card.TargetType targetType, int targetCount,
                                   List<String> effects, boolean exhaust,
                                   boolean retain, boolean ethereal,
                                   String profession) {
        return new Card(id, name, cost, description, type, rarity, value,
                targetType, targetCount, effects, exhaust, retain, ethereal, profession);
    }

    /**
     * 获取指定职业的所有卡牌。
     *
     * @param profession 职业标识 "warrior"/"mage"/"witch"
     * @return 卡牌列表（不可修改）
     */
    public static List<Card> getCardsByProfession(String profession) {
        return poolByProfession.getOrDefault(profession, new ArrayList<>());
    }

    /**
     * 获取所有注册的卡牌。
     *
     * @return 所有卡牌列表
     */
    public static List<Card> getAllCards() {
        return new ArrayList<>(allCards);
    }

    /**
     * 按职业和稀有度获取卡牌。
     *
     * @param profession 职业
     * @param rarity     稀有度
     * @return 符合条件的卡牌列表
     */
    public static List<Card> getCardsByProfessionAndRarity(String profession,
                                                            Card.Rarity rarity) {
        return poolByProfession.getOrDefault(profession, new ArrayList<>())
                .stream()
                .filter(c -> c.getRarity() == rarity)
                .toList();
    }

    /**
     * 按名称查找卡牌。
     *
     * @param name 卡牌名称
     * @return 匹配的卡牌，未找到返回 null
     */
    public static Card findByName(String name) {
        return allCards.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 按 ID 查找卡牌。
     *
     * @param id 卡牌 ID
     * @return 匹配的卡牌，未找到返回 null
     */
    public static Card findById(String id) {
        return allCards.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从卡牌池中随机选择指定数量的卡牌。
     *
     * @param pool       源卡牌池
     * @param count      选择数量
     * @return 随机选中的卡牌列表
     */
    public static List<Card> randomSelect(List<Card> pool, int count) {
        return randomSelect(pool, count, RANDOM);
    }

    public static List<Card> randomSelect(List<Card> pool, int count, Random random) {
        List<Card> copy = new ArrayList<>(pool);
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < count && !copy.isEmpty(); i++) {
            int idx = random.nextInt(copy.size());
            result.add(copy.remove(idx));
        }
        return result;
    }

    /**
     * 手动注册卡牌（供 JSON 加载使用）。
     *
     * @param profession 职业
     * @param cards      卡牌列表
     */
    public static void registerCards(String profession, List<Card> cards) {
        poolByProfession.computeIfAbsent(profession, k -> new ArrayList<>())
                .addAll(cards);
        allCards.addAll(cards);
    }
}
