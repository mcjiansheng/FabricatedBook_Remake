package com.fabricatedbook.core.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enemy — 怪物实体
 * <p>
 * 继承自 AbstractEntity，代表战斗中的敌人。
 * 额外持有意图类型和行动脚本。
 * 行动脚本定义了怪物在不同回合可能执行的动作。
 * <p>
 * 引用方：EntityFactory（从 JSON 创建怪物）、CombatEngine（AI 决策）、
 *         BattleScreen（显示怪物状态）、EnemyActor（渲染）
 */
public class Enemy extends AbstractEntity {

    /** 当前意图 */
    private IntentType intent;

    /** 行动脚本列表 — 每个元素是一个动作的字符串标识 */
    private List<String> actionScript;

    /** 当前行动索引 */
    private int actionIndex;

    /** 数据配置中的被动效果标识。 */
    private String passive;

    /** 已触发的一次性被动标记。 */
    private final Set<String> passiveFlags;

    /**
     * 构造怪物实体。
     *
     * @param id           怪物唯一标识符
     * @param name         怪物显示名称
     * @param maxHp        最大生命值
     * @param actionScript 行动脚本列表
     */
    public Enemy(String id, String name, int maxHp, List<String> actionScript) {
        this(id, name, maxHp, actionScript, "");
    }

    public Enemy(String id, String name, int maxHp, List<String> actionScript,
                 String passive) {
        super(id, name, maxHp);
        this.intent = IntentType.UNKNOWN;
        this.actionScript = actionScript;
        this.actionIndex = 0;
        this.passive = passive != null ? passive : "";
        this.passiveFlags = new HashSet<>();
    }

    // ====== 意图相关 ======

    public IntentType getIntent() { return intent; }
    public void setIntent(IntentType intent) { this.intent = intent; }

    // ====== 行动脚本相关 ======

    public List<String> getActionScript() { return actionScript; }
    public void setActionScript(List<String> actionScript) {
        this.actionScript = actionScript;
    }

    public int getActionIndex() { return actionIndex; }
    public void setActionIndex(int actionIndex) { this.actionIndex = actionIndex; }

    /**
     * 获取当前回合要执行的行动标识。
     * 循环使用行动脚本（到达末尾后回到第一个）。
     *
     * @return 当前行动标识字符串
     */
    public String getCurrentAction() {
        if (actionScript == null || actionScript.isEmpty()) {
            return "atk1";
        }
        String action = peekCurrentAction();
        actionIndex = (actionIndex + 1) % actionScript.size();
        return action;
    }

    /**
     * 预览当前回合行动，不推进行动脚本。
     *
     * @return 当前行动标识字符串
     */
    public String peekCurrentAction() {
        if (actionScript == null || actionScript.isEmpty()) {
            return "atk1";
        }
        return actionScript.get(actionIndex % actionScript.size());
    }

    /**
     * 根据行动标识自动设置意图类型。
     *
     * @param actionId 行动标识
     */
    public void deduceIntent(String actionId) {
        if (actionId == null) {
            this.intent = IntentType.UNKNOWN;
            return;
        }
        if (actionId.startsWith("atk") || actionId.startsWith("attack")
                || actionId.contains("_attack")) {
            this.intent = IntentType.ATTACK;
        } else if (actionId.startsWith("def") || actionId.startsWith("block")) {
            this.intent = IntentType.DEFEND;
        } else if (actionId.startsWith("buff") || actionId.startsWith("inc")
                || actionId.startsWith("heal")) {
            this.intent = IntentType.BUFF;
        } else if (actionId.startsWith("debuff") || actionId.startsWith("curse")) {
            this.intent = IntentType.DEBUFF;
        } else {
            this.intent = IntentType.UNKNOWN;
        }
    }

    /**
     * 判断怪物是否拥有特殊被动（根据怪物 ID 判断）。
     *
     * @param passiveName 被动名称
     * @return true 如果有该被动
     */
    public boolean hasPassive(String passiveName) {
        return passiveName != null && passive.equalsIgnoreCase(passiveName);
    }

    public String getPassive() { return passive; }
    public void setPassive(String passive) {
        this.passive = passive != null ? passive : "";
    }

    public boolean hasAnyPassive() {
        return passive != null && !passive.isBlank();
    }

    public boolean markPassiveTriggered(String flag) {
        return passiveFlags.add(flag);
    }

    @Override
    public String toString() {
        return "Enemy{" + name +
                " HP:" + hp + "/" + maxHp +
                " Intent:" + intent +
                "}";
    }
}
