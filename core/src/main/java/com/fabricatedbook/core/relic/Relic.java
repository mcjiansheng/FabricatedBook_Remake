package com.fabricatedbook.core.relic;

/**
 * Relic — 藏品接口
 * <p>
 * 所有藏品实现此接口。
 * 每个藏品有名称、效果描述和稀有度。
 * 藏品通过 EventBus 订阅游戏事件来触发效果。
 * <p>
 * 引用方：RelicManager（管理藏品）、Player（持有藏品列表）、
 *         ShopManager（出售藏品）、各种藏品的具体实现
 */
public interface Relic {

    /**
     * 稀珍度枚举（与游戏百科一致）。
     */
    enum Rarity {
        /** ⚪ 普通 — 价值 0 */
        COMMON(0, "普通"),
        /** 🔵 稀有 — 价值 1 */
        UNCOMMON(1, "稀有"),
        /** 🟣 史诗 — 价值 2 */
        EPIC(2, "史诗"),
        /** 🟡 传说 — 价值 3 */
        LEGENDARY(3, "传说"),
        /** 🔴 神话 — 价值 4 */
        MYTHIC(4, "神话"),
        /** ⚖️ 特殊（背叛/仇恨等） */
        SPECIAL(0, "特殊"),
        /** 🌊 负面藏品 */
        CURSED(0, "负面");

        private final int value;
        private final String displayName;

        Rarity(int value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public int getValue() { return value; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * 获取藏品名称。
     *
     * @return 名称字符串
     */
    String getName();

    /**
     * 获取藏品效果描述。
     *
     * @return 描述字符串
     */
    String getDescription();

    /**
     * 获取藏品稀珍度。
     *
     * @return 稀珍度枚举
     */
    Rarity getRarity();

    /**
     * 订阅藏品效果到事件总线。
     * <p>
     * 在战斗开始时由 RelicManager 调用，
     * 藏品在此方法中注册到相关事件。
     *
     * @param bus 事件总线实例
     */
    void subscribe(EventBus bus);

    /**
     * 从事件总线取消订阅。
     * <p>
     * 战斗结束后调用，清理事件订阅。
     *
     * @param bus 事件总线实例
     */
    void unsubscribe(EventBus bus);

    /**
     * 获取藏品 ID（用于 JSON 配置映射）。
     *
     * @return 藏品 ID
     */
    String getId();
}
