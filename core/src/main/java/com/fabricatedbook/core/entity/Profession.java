package com.fabricatedbook.core.entity;

/**
 * Profession — 职业枚举
 * <p>
 * 定义游戏中可选的三种职业。
 * 每个职业有专属的卡牌池和初始属性。
 * <p>
 * 引用方：Player（持有职业类型）、CardPool（按职业筛选卡牌）
 */
public enum Profession {

    /** 🗡️ 战士 — 战斗胜利回复 6-12 点生命值，生命值上限 80 */
    WARRIOR("战士", 80),

    /** 🔮 法师 — （待补充具体特性） */
    MAGE("法师", 60),

    /** 🧙 女巫 — （待补充具体特性） */
    WITCH("女巫", 60);

    /** 职业中文名称 */
    private final String displayName;

    /** 初始生命值上限 */
    private final int baseMaxHp;

    /**
     * 构造职业枚举。
     *
     * @param displayName 中文显示名称
     * @param baseMaxHp   初始生命值上限
     */
    Profession(String displayName, int baseMaxHp) {
        this.displayName = displayName;
        this.baseMaxHp = baseMaxHp;
    }

    /**
     * 获取职业中文名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取该职业的初始生命值上限。
     *
     * @return 基础最大生命值
     */
    public int getBaseMaxHp() {
        return baseMaxHp;
    }
}
