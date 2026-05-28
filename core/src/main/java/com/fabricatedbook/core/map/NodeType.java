package com.fabricatedbook.core.map;

/**
 * NodeType — 地图节点类型枚举
 * <p>
 * 定义地图中所有可能的节点类型。
 * 每种类型有对应的显示颜色、图标和交互方式。
 * <p>
 * 引用方：Node（持有节点类型）、MapConfig（配置概率表）、
 *         NodeFactory（创建节点）、MapGraph（构建地图）
 */
public enum NodeType {

    /** 🗡️ 战斗 — 常规战斗，胜利获得金币和卡牌 */
    FIGHT,

    /** ⚠️ 紧急作战 — 有难度增益的战斗，胜利获得藏品+金币+卡牌 */
    EMERGENCY,

    /** 👹 Boss — 层底首领战 */
    BOSS,

    /** ❓ 不期而遇 — 随机事件 */
    UNEXPECTED,

    /** 🎁 得偿所愿 — 选择藏品 */
    REWARD,

    /** 🏪 诡异行商 — 商店 */
    SHOP,

    /** 🔀 命运抉择 — 根据选择更改探索方向 */
    DECISION,

    /** 🏠 安全屋 — 获得补给 */
    SAFEHOUSE;

    /**
     * 判断该节点类型是否为战斗节点。
     *
     * @return true 如果是战斗类节点
     */
    public boolean isCombat() {
        return this == FIGHT || this == EMERGENCY || this == BOSS;
    }

    /**
     * 判断该节点是否为事件节点。
     *
     * @return true 如果是事件类节点
     */
    public boolean isEvent() {
        return this == UNEXPECTED || this == DECISION;
    }

    /**
     * 获取节点类型的中文名称。
     *
     * @return 中文名称
     */
    public String getDisplayName() {
        switch (this) {
            case FIGHT: return "战斗";
            case EMERGENCY: return "紧急作战";
            case BOSS: return "Boss";
            case UNEXPECTED: return "不期而遇";
            case REWARD: return "得偿所愿";
            case SHOP: return "诡异行商";
            case DECISION: return "命运抉择";
            case SAFEHOUSE: return "安全屋";
            default: return "未知";
        }
    }
}
