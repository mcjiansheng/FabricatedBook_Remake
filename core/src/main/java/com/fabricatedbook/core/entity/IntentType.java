package com.fabricatedbook.core.entity;

/**
 * IntentType — 敌人意图枚举
 * <p>
 * 定义敌人在当前回合会执行的行动类型。
 * 用于在战斗界面显示敌人的意图图标，辅助玩家决策。
 * <p>
 * 引用方：Enemy（持有当前意图）、EnemyActor（显示意图图标）
 */
public enum IntentType {

    /** 🗡️ 攻击 — 将对玩家造成伤害 */
    ATTACK,

    /** 🛡️ 防御 — 将获得格挡或防御性增益 */
    DEFEND,

    /** 💪 增益 — 将对自己施加 Buff（力量、抗性等） */
    BUFF,

    /** 🟣 减益 — 将对玩家施加 Debuff */
    DEBUFF,

    /** ❓ 未知 — 无法预知的行动（BOSS 或特殊敌人） */
    UNKNOWN
}
