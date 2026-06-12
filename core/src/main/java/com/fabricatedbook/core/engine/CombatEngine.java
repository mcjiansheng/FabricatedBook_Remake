package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.*;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.event.OnCombatStart;
import com.fabricatedbook.core.event.OnEntityDeath;
import com.fabricatedbook.core.event.OnTurnEnd;
import com.fabricatedbook.core.event.OnCardUsed;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.relic.EventBus;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * CombatEngine — 战斗主控引擎
 * <p>
 * 管理整场战斗的生命周期：初始化、回合流程、卡牌结算、胜负判定。
 * 与 ActionManager 配合生成和执行 CombatAction。
 * 每回合流程：分发能量 → 抽牌 → 玩家操作 → 敌人行动 → 回合末结算。
 * <p>
 * 引用方：GameScreen（用户交互时调用 playCard/endRound）、
 *         RelicManager（战斗开始/结束时触发）、
 *         DamageCalculator（伤害计算）、ActionManager（动作队列）
 */
public class CombatEngine {

    /** 玩家实体 */
    private Player player;

    /** 敌人列表 */
    private List<Enemy> enemies;

    /** 动作队列管理器 */
    private ActionManager actionManager;

    /** 藏品管理器 */
    private RelicManager relicManager;

    /** 视图通知器 */
    private ViewNotifier viewNotifier;

    /** 当前回合数 */
    private int turn;

    /** 是否正在战斗中 */
    private boolean inBattle;

    /** 玩家是否胜利 */
    private boolean victory;

    /** 随机数生成器 */
    private final Random random;

    /** 事件总线 */
    private EventBus eventBus;

    /** 当前战斗来源节点类型，用于结算正式奖励区间。 */
    private NodeType battleNodeType;

    /**
     * 构造战斗引擎。
     */
    public CombatEngine() {
        this.actionManager = new ActionManager();
        this.random = new Random();
        this.turn = 0;
        this.inBattle = false;
        this.victory = false;
        this.eventBus = EventBus.getInstance();
        this.battleNodeType = NodeType.FIGHT;
    }

    /**
     * 设置视图通知器。
     *
     * @param notifier 视图通知实例
     */
    public void setViewNotifier(ViewNotifier notifier) {
        this.viewNotifier = notifier;
    }

    // ==================== 战斗生命周期 ====================

    /**
     * 初始化战斗。
     * <p>
     * 分配玩家和敌人，生成初始手牌，注册藏品事件，通知视图。
     *
     * @param player  玩家实体
     * @param enemies 敌人列表
     */
    public void initBattle(Player player, List<Enemy> enemies) {
        this.player = player;
        this.enemies = new ArrayList<>(enemies);
        this.actionManager.clear();
        this.turn = 0;
        this.inBattle = true;
        this.victory = false;

        // 初始化玩家手牌：抽牌堆放入基础卡牌，抽初始手牌
        initPlayerDeck();

        // 藏品管理器事件订阅
        if (relicManager != null) {
            relicManager.onCombatStart();
        }

        // 通知视图
        if (viewNotifier != null) {
            List<AbstractEntity> enemyEntities = new ArrayList<>(enemies);
            viewNotifier.onBattleStart(player, enemyEntities);
        }

        // 自动开始第一回合
        startRound();
    }

    /**
     * 初始化玩家牌组：将玩家卡牌牌组放入抽牌堆并洗牌。
     */
    private void initPlayerDeck() {
        // 如果没有抽牌堆，创建初始套牌
        if (player.getDrawPile().isEmpty()) {
            List<Card> starterDeck = new ArrayList<>();
            // 基础攻击牌 ×5
            for (int i = 0; i < 5; i++) {
                Card atk = CardPool.findById("war_atk1");
                if (atk != null) starterDeck.add(new Card(atk.getId(), atk.getName(),
                        atk.getCost(), atk.getDescription(), atk.getType(),
                        atk.getRarity(), atk.getValue(), atk.getTargetType(),
                        atk.getTargetCount(), new ArrayList<>(atk.getEffects()),
                        atk.isExhaust(), atk.getProfession()));
            }
            // 基础防御牌 ×5
            for (int i = 0; i < 5; i++) {
                Card def = CardPool.findById("war_def1");
                if (def != null) starterDeck.add(new Card(def.getId(), def.getName(),
                        def.getCost(), def.getDescription(), def.getType(),
                        def.getRarity(), def.getValue(), def.getTargetType(),
                        def.getTargetCount(), new ArrayList<>(def.getEffects()),
                        def.isExhaust(), def.getProfession()));
            }
            player.getDrawPile().addAll(starterDeck);
        }
        Collections.shuffle(player.getDrawPile());
    }

    /**
     * 开始新回合。
     * <p>
     * 1. 回合计数 +1
     * 2. 重置能量（3 基础 + ExtraEnergyBuff）
     * 3. 清除非装甲格挡
     * 4. 抽 5 张牌
     * 5. 检查是否眩晕
     * 6. 调用 BuffHook.onTurnStart
     * 7. 敌人设置意图
     */
    public void startRound() {
        if (!inBattle) return;

        turn++;
        System.out.println("[CombatEngine] === 第 " + turn + " 回合 ===");

        // 1. 计算可用能量（基础 + ExtraEnergyBuff）
        int energyAmount = player.getMaxEnergy();
        for (BuffHook buff : player.getBuffs()) {
            if (buff.getBuffName().equals("ExtraEnergy")) {
                energyAmount += buff.getStack();
            }
        }
        player.setEnergy(energyAmount);

        // 2. 清理非装甲格挡（只有装甲Buff的保留格挡）
        // 简化：掉线时不清除格挡并交由 ArmorBuff.onTurnEnd 控制
        // 默认清除所有格挡，装甲Buff会阻止
        boolean hasArmor = player.hasBuff("Armor");
        if (!hasArmor) {
            player.clearBlock();
        }

        // 3. 抽牌默认为5张
        int drawCount = 5;
        // 检查眩晕：眩晕则跳过回合
        if (player.isDizzy()) {
            drawCount = 0;
            player.setDizzy(false);
            System.out.println("[CombatEngine] 玩家眩晕，跳过回合");
        }

        // 4. 调用 BuffHook.onTurnStart
        for (BuffHook buff : player.getBuffs()) {
            buff.onTurnStart(player);
        }
        for (Enemy enemy : enemies) {
            for (BuffHook buff : enemy.getBuffs()) {
                buff.onTurnStart(enemy);
            }
        }

        // 5. 清理过期 Buff
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }

        // 6. 抽牌
        player.drawCards(drawCount);

        // 7. 通知视图
        if (viewNotifier != null) {
            viewNotifier.onTurnStart(turn);
        }

        // 8. 检查敌人意图
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                String actionId = enemy.peekCurrentAction();
                enemy.deduceIntent(actionId);
            }
        }

        // 检查战斗是否结束（某些Buff可能导致死亡）
        checkBattleEnd();
    }

    /**
     * 玩家使用卡牌。
     * <p>
     * 1. 检查能量是否充足
     * 2. 从手牌移除卡牌
     * 3. 解析卡牌效果并生成 CombatAction 队列
     * 4. 执行动作队列
     * 5. 如果卡牌不消耗，将其放入弃牌堆
     *
     * @param card   要使用的卡牌
     * @param target 目标敌人（null 表示自身目标）
     * @return true 如果成功使用卡牌
     */
    public boolean playCard(Card card, Enemy target) {
        if (!inBattle || card == null) return false;

        // 检查能量
        if (player.getEnergy() < card.getCost()) {
            System.out.println("[CombatEngine] 能量不足，需要 " + card.getCost()
                    + "，当前 " + player.getEnergy());
            return false;
        }

        // 从手牌移除。手牌可能有多张同 ID 基础牌，必须按实例移除。
        if (!removeCardInstanceFromHand(card)) {
            return false;
        }

        // 消耗能量
        player.spendEnergy(card.getCost());

        // 解析卡牌效果
        List<CombatAction> actions = parseCardEffects(card, target);

        // 将动作加入队列
        for (CombatAction action : actions) {
            queueAction(action);
        }

        // 处理消耗
        if (card.isExhaust()) {
            // 消耗牌：从游戏中移除，不进入弃牌堆
            System.out.println("[CombatEngine] 消耗牌: " + card.getName());
        } else {
            player.getDiscardPile().add(card);
        }

        // 执行队列
        executeActionQueue();

        if (relicManager != null) {
            relicManager.fireCardUsed(card, card.getCost());
        }

        // 通知视图
        if (viewNotifier != null && !actions.isEmpty()) {
            viewNotifier.onCardPlayed(actions.get(0));
        }

        // 检查战斗状态
        checkBattleEnd();

        return true;
    }

    /**
     * 解析卡牌效果生成 CombatAction 列表。
     * <p>
     * 效果字符串格式说明：
     * - "damage:N" -> 对单目标造成 N 点伤害
     * - "damage_all:N" -> 对所有敌人造成 N 点伤害
     * - "damage:N:M" -> 对单目标造成 N 点伤害，重复 M 次
     * - "damage_all:N:M" -> 对所有敌人造成 N 点伤害，重复 M 次
     * - "block:N" -> 获得 N 格挡
     * - "heal:N" -> 回复 N 生命值
     * - "draw:N" -> 抽 N 张牌
     * - "energy:N" -> 获得 N 能量
     * - "debuff:name:N" -> 对目标施加 name N 层
     * - "debuff_all:name:N" -> 对所有敌人施加 name N 层
     * - "buff:self:name:N" -> 对自己施加 name N 层
     * - "purify" -> 清除所有负面效果
     * - "counter:block" -> 根据格挡值造成伤害
     * - "bonus_per_attack:N" -> 手牌每张攻击牌附加 N 伤害
     * - "bonus_low_hp:threshold:bonus" -> 敌人血量低于阈值则增加伤害
     * - "escalating:N" -> 每用一次该牌增加 N 伤害（通过卡牌状态）
     * - "detonate_withering:N" -> 引爆 N 次凋零
     * - "double_poison" -> 敌人中毒层数翻倍
     * - "poison_chance:chance:amount" -> 每次攻击 chance% 概率附加 amount 中毒
     * - "block_per_target:N" -> 每个目标提供 N 格挡
     * - "bonus_per_damage_taken:threshold:bonus" -> 每损失 threshold 生命值，伤害 +bonus
     * - "add_random_attack" -> 随机获得一张攻击牌
     * - "stun_chance:N" -> N% 概率眩晕
     *
     * @param card   使用的卡牌
     * @param target 目标敌人（可为 null）
     * @return CombatAction 列表
     */
    private List<CombatAction> parseCardEffects(Card card, Enemy target) {
        List<CombatAction> actions = new ArrayList<>();
        List<AbstractEntity> aliveEnemies = getAliveEnemies();

        for (String effect : card.getEffects()) {
            String[] parts = effect.split(":");
            String type = parts[0];

            switch (type) {
                case "damage": {
                    // damage:N 或 damage:N:M
                    int dmg = Integer.parseInt(parts[1]);
                    int repeat = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    for (int i = 0; i < repeat; i++) {
                        List<AbstractEntity> singleTarget = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            singleTarget.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            singleTarget.add(aliveEnemies.get(0));
                        }
                        if (!singleTarget.isEmpty()) {
                            actions.add(new DamageAction(player, singleTarget, dmg, repeat > 1));
                        }
                    }
                    break;
                }
                case "damage_all": {
                    // damage_all:N 或 damage_all:N:M
                    int dmg = Integer.parseInt(parts[1]);
                    int repeat = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    for (int i = 0; i < repeat; i++) {
                        actions.add(new DamageAction(player,
                                new ArrayList<>(aliveEnemies), dmg, repeat > 1));
                    }
                    break;
                }
                case "block": {
                    int block = Integer.parseInt(parts[1]);
                    actions.add(new GainBlockAction(player, block));
                    break;
                }
                case "heal": {
                    int amount = Integer.parseInt(parts[1]);
                    actions.add(new HealAction(player, amount));
                    break;
                }
                case "draw": {
                    int count = Integer.parseInt(parts[1]);
                    actions.add(new DrawCardAction(player, count));
                    break;
                }
                case "energy": {
                    int amount = Integer.parseInt(parts[1]);
                    actions.add(new GainEnergyAction(player, amount));
                    break;
                }
                case "debuff": {
                    // debuff:name:stack
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    List<AbstractEntity> singleTarget = new ArrayList<>();
                    if (target != null && target.isAlive()) {
                        singleTarget.add(target);
                    } else if (!aliveEnemies.isEmpty()) {
                        singleTarget.add(aliveEnemies.get(0));
                    }
                    if (!singleTarget.isEmpty()) {
                        actions.add(new ApplyBuffAction(singleTarget.get(0), buffName, stack));
                    }
                    break;
                }
                case "debuff_all": {
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    for (AbstractEntity enemy : aliveEnemies) {
                        actions.add(new ApplyBuffAction(enemy, buffName, stack));
                    }
                    break;
                }
                case "buff": {
                    // buff:self:name:stack
                    String buffName = parts[2];
                    int stack = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;
                    actions.add(new ApplyBuffAction(player, buffName, stack));
                    break;
                }
                case "purify": {
                    // 清除所有负面效果
                    for (BuffHook buff : new ArrayList<>(player.getBuffs())) {
                        String bName = buff.getBuffName();
                        if (bName.equals("Fragile") || bName.equals("BlockReduction")
                                || bName.equals("Weak") || bName.equals("Poison")
                                || bName.equals("Withering") || bName.equals("Dizziness")) {
                            player.removeBuff(bName);
                        }
                    }
                    break;
                }
                case "counter": {
                    // 反击：根据格挡值造成伤害
                    int blockDamage = player.getBlock();
                    if (blockDamage > 0) {
                        List<AbstractEntity> targets = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            targets.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            targets.add(aliveEnemies.get(0));
                        }
                        if (!targets.isEmpty()) {
                            actions.add(new DamageAction(player, targets, blockDamage));
                        }
                    }
                    break;
                }
                case "bonus_per_attack": {
                    // 手牌中每张攻击牌附加伤害
                    int bonus = Integer.parseInt(parts[1]);
                    int attackCount = (int) player.getHand().stream()
                            .filter(c -> c.getType() == Card.CardType.ATTACK)
                            .count();
                    // 修正前一个damage动作的伤害
                    if (!actions.isEmpty() && actions.get(actions.size() - 1) instanceof DamageAction) {
                        // 无法直接修改已有DamageAction，新加一个DamageAction
                        List<AbstractEntity> targets = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            targets.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            targets.add(aliveEnemies.get(0));
                        }
                        actions.add(new DamageAction(player, targets, bonus * attackCount));
                    }
                    break;
                }
                case "bonus_low_hp": {
                    int threshold = Integer.parseInt(parts[1]);
                    int bonus = Integer.parseInt(parts[2]);
                    if (target != null && target.getHp() < threshold && target.isAlive()) {
                        actions.add(new DamageAction(player,
                                List.of(target), bonus));
                    }
                    break;
                }
                case "detonate_withering": {
                    int times = Integer.parseInt(parts[1]);
                    if (target != null && target.isAlive()) {
                        for (int i = 0; i < times; i++) {
                            // 查找目标身上的凋零Buff并引爆
                            for (BuffHook buff : new ArrayList<>(target.getBuffs())) {
                                if (buff.getBuffName().equals("Withering") && buff.getStack() > 0) {
                                    int witherDmg = buff.getStack() * 2; // 凋零伤害公式
                                    target.takeDamage(witherDmg);
                                    target.removeBuff("Withering");
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
                case "double_poison": {
                    for (AbstractEntity enemy : aliveEnemies) {
                        for (BuffHook buff : new ArrayList<>(enemy.getBuffs())) {
                            if (buff.getBuffName().equals("Poison")) {
                                int newStack = buff.getStack() * 2;
                                enemy.removeBuff("Poison");
                                actions.add(new ApplyBuffAction(enemy, "Poison", newStack));
                                break;
                            }
                        }
                    }
                    break;
                }
                case "block_per_target": {
                    int block = Integer.parseInt(parts[1]);
                    actions.add(new GainBlockAction(player, block * aliveEnemies.size()));
                    break;
                }
                case "bonus_per_damage_taken": {
                    int threshold = Integer.parseInt(parts[1]);
                    int bonus = Integer.parseInt(parts[2]);
                    int lostHp = player.getMaxHp() - player.getHp();
                    int extraDmg = (lostHp / threshold) * bonus;
                    if (extraDmg > 0) {
                        List<AbstractEntity> targets = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            targets.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            targets.add(aliveEnemies.get(0));
                        }
                        actions.add(new DamageAction(player, targets, extraDmg));
                    }
                    break;
                }
                case "add_random_attack": {
                    List<Card> attackCards = CardPool.getCardsByProfession(player.getProfession().name().toLowerCase())
                            .stream().filter(c -> c.getType() == Card.CardType.ATTACK).toList();
                    if (!attackCards.isEmpty()) {
                        Card randomCard = attackCards.get(random.nextInt(attackCards.size()));
                        if (randomCard != null) {
                            player.getHand().add(new Card(randomCard.getId(), randomCard.getName(),
                                    randomCard.getCost(), randomCard.getDescription(),
                                    randomCard.getType(), randomCard.getRarity(),
                                    randomCard.getValue(), randomCard.getTargetType(),
                                    randomCard.getTargetCount(),
                                    new ArrayList<>(randomCard.getEffects()),
                                    randomCard.isExhaust(), randomCard.getProfession()));
                        }
                    }
                    break;
                }
                case "stun_chance": {
                    int chance = Integer.parseInt(parts[1]);
                    if (random.nextInt(100) < chance && target != null && target.isAlive()) {
                        target.setDizzy(true);
                    }
                    break;
                }
                case "escalating": {
                    // escalating:N — 每使用一次该牌，伤害 +N（通过卡牌实例累计）
                    int bonus = Integer.parseInt(parts[1]);
                    card.addEscalatingBonus(bonus);
                    List<AbstractEntity> escTargets = new ArrayList<>();
                    if (target != null && target.isAlive()) {
                        escTargets.add(target);
                    } else if (!aliveEnemies.isEmpty()) {
                        escTargets.add(aliveEnemies.get(0));
                    }
                    if (!escTargets.isEmpty() && card.getEscalatingBonus() > 0) {
                        actions.add(new DamageAction(player, escTargets,
                                card.getEscalatingBonus() - bonus)); // 本次之前已累计的额外伤害
                    }
                    break;
                }
                case "chance_debuff": {
                    // chance_debuff:BuffName:Stack:Chance
                    String buffName = parts[1];
                    int stack = Integer.parseInt(parts[2]);
                    int chance = Integer.parseInt(parts[3]);
                    if (random.nextInt(100) < chance) {
                        List<AbstractEntity> cdTargets = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            cdTargets.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            cdTargets.add(aliveEnemies.get(0));
                        }
                        if (!cdTargets.isEmpty()) {
                            actions.add(new ApplyBuffAction(cdTargets.get(0), buffName, stack));
                        }
                    }
                    break;
                }
                case "trigger_withering": {
                    // trigger_withering 或 trigger_withering:N — 引爆凋零
                    int times = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (target != null && target.isAlive()) {
                        for (int i = 0; i < times; i++) {
                            for (BuffHook buff : new ArrayList<>(target.getBuffs())) {
                                if (buff.getBuffName().equals("Withering") && buff.getStack() > 0) {
                                    int witherDmg = buff.getStack() * 2;
                                    target.takeDamage(witherDmg);
                                    target.removeBuff("Withering");
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
                default:
                    System.out.println("[CombatEngine] 未知效果: " + effect);
                    break;
            }
        }

        return actions;
    }

    /**
     * 执行动作队列中的所有动作。
     */
    private void executeActionQueue() {
        while (!actionManager.isAllFinished()) {
            CombatAction action = actionManager.executeNext();
            if (viewNotifier != null && action != null) {
                viewNotifier.onActionExecuted(action);
            }
        }
    }

    /**
     * 结束当前回合。
     * <p>
     * 1. 调用 BuffHook.onTurnEnd
     * 2. 弃掉所有手牌
     * 3. 敌人执行行动
     * 4. 检查战斗状态
     */
    public void endRound() {
        if (!inBattle) return;

        System.out.println("[CombatEngine] --- 第 " + turn + " 回合结束 ---");

        // 1. 调用 Buff.onTurnEnd
        for (BuffHook buff : player.getBuffs()) {
            buff.onTurnEnd(player);
        }
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                for (BuffHook buff : enemy.getBuffs()) {
                    buff.onTurnEnd(enemy);
                }
            }
        }

        // 2. 弃手牌
        player.discardHand();

        // 3. 清理过期 Buff
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }

        // 4. 对手执行行动
        executeEnemyTurns();

        // 5. 清理敌人逾期 Buff
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }

        // 6. 发布回合结束事件
        eventBus.publish(new OnTurnEnd(player.getId(), player));
        if (relicManager != null) {
            relicManager.fireTurnEnd(player);
        }

        // 7. 检查战斗是否结束
        checkBattleEnd();

        // 8. 如果战斗未结束，开始下一回合
        if (inBattle) {
            startRound();
        }
    }

    /**
     * 执行所有存活敌人的行动。
     */
    private void executeEnemyTurns() {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            String actionId = enemy.getCurrentAction();
            System.out.println("[CombatEngine] " + enemy.getName() + " 行动: " + actionId);

            // 解析敌人动作
            List<CombatAction> actions = parseEnemyAction(enemy, actionId);
            for (CombatAction action : actions) {
                queueAction(action);
            }
        }

        executeActionQueue();
    }

    /**
     * 解析敌人行动标识生成动作列表。
     * <p>
     * 动作模式：
     * atkX -> 攻击造成 X 点伤害
     * atkXxY -> 攻击造成 X×Y 点伤害
     * inc -> 增益（力量/坚强）
     * defX -> 获得 X 格挡
     *
     * @param enemy    敌人实体
     * @param actionId 行动标识
     * @return 动作列表
     */
    private List<CombatAction> parseEnemyAction(Enemy enemy, String actionId) {
        List<CombatAction> actions = new ArrayList<>();
        if (actionId == null) return actions;

        // 优先使用 EnemyActionResolver 解析原版动作名
        List<CombatAction> resolved = EnemyActionResolver.resolve(
                enemy, actionId, player, getAliveEnemyList(), random);
        if (resolved != null) return resolved;

        try {
            if (actionId.startsWith("atk")) {
                // atkX 或 atkXxY
                String numPart = actionId.substring(3);
                if (numPart.contains("x")) {
                    String[] parts = numPart.split("x");
                    int dmg = Integer.parseInt(parts[0]);
                    int times = Integer.parseInt(parts[1]);
                    for (int i = 0; i < times; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), dmg, times > 1));
                    }
                } else {
                    int dmg = Integer.parseInt(numPart);
                    actions.add(new DamageAction(enemy, List.of(player), dmg));
                }
            } else if (actionId.startsWith("attack")) {
                String numPart = actionId.substring(6);
                if (numPart.contains("x")) {
                    String[] parts = numPart.split("x");
                    int dmg = Integer.parseInt(parts[0]);
                    int times = Integer.parseInt(parts[1]);
                    for (int i = 0; i < times; i++) {
                        actions.add(new DamageAction(enemy, List.of(player), dmg, times > 1));
                    }
                } else {
                    int dmg = Integer.parseInt(numPart);
                    actions.add(new DamageAction(enemy, List.of(player), dmg));
                }
            } else if (actionId.startsWith("def") || actionId.startsWith("block")) {
                String numPart = actionId.substring(3);
                if (actionId.startsWith("block")) {
                    numPart = actionId.substring(5);
                }
                int block = Integer.parseInt(numPart);
                actions.add(new GainBlockAction(enemy, block));
            } else if (actionId.startsWith("buff") || actionId.startsWith("inc")) {
                // buff:strength:turn 或 inc
                String[] parts = actionId.split(":");
                if (parts.length >= 3) {
                    actions.add(new ApplyBuffAction(enemy, parts[1], Integer.parseInt(parts[2])));
                } else {
                    actions.add(new ApplyBuffAction(enemy, "Strength", 3));
                }
            } else if (actionId.startsWith("debuff") || actionId.startsWith("curse")) {
                String[] parts = actionId.split(":");
                if (parts.length >= 3) {
                    actions.add(new ApplyBuffAction(enemy, parts[1], Integer.parseInt(parts[2])));
                } else {
                    actions.add(new ApplyBuffAction(enemy, "Weak", 3));
                }
            } else if (actionId.startsWith("heal")) {
                String numPart = actionId.substring(4);
                actions.add(new HealAction(enemy, Integer.parseInt(numPart)));
            } else if ("idle".equals(actionId) || "stun".equals(actionId)) {
                // 什么都不做
            } else {
                // 默认：攻击
                actions.add(new DamageAction(enemy, List.of(player), 6));
            }
        } catch (Exception e) {
            System.err.println("[CombatEngine] 解析敌人动作失败: " + actionId);
            actions.add(new DamageAction(enemy, List.of(player), 6));
        }

        return actions;
    }

    /**
     * 检查战斗结果。
     * <p>
     * - 所有敌人死亡 → 玩家胜利
     * - 玩家死亡 → 玩家失败
     */
    public void checkBattleEnd() {
        if (!inBattle) return;

        // 检查玩家是否死亡
        if (!player.isAlive()) {
            inBattle = false;
            victory = false;
            System.out.println("[CombatEngine] 战斗失败！玩家阵亡。");

            if (relicManager != null) {
                relicManager.onCombatEnd();
            }

            if (viewNotifier != null) {
                viewNotifier.onBattleEnd(false, null);
            }
            return;
        }

        // 检查所有敌人是否死亡
        boolean allDead = true;
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                allDead = false;
                break;
            }
        }

        if (allDead) {
            inBattle = false;
            victory = true;

            boolean finalBoss = battleNodeType == NodeType.BOSS && player.getCurrentFloor() == 5;
            int goldReward = finalBoss ? 0 : calculateGoldReward();
            if (relicManager != null) {
                goldReward = relicManager.modifyGoldReward(goldReward);
            }
            if (goldReward > 0) {
                player.gainGold(goldReward);
            }

            int healAmount = finalBoss ? 0 : player.battleRewardHeal();

            // 发布死亡事件
            for (Enemy enemy : enemies) {
                eventBus.publish(new OnEntityDeath(player.getId(), enemy, player));
                if (relicManager != null) {
                    relicManager.fireEntityDeath(enemy, player);
                }
            }

            // 藏品结束
            if (relicManager != null) {
                relicManager.onCombatEnd();
            }

            String reward = finalBoss ? "最终 Boss 已击败，进入结局" : "获得 " + goldReward + " 金币";
            if (healAmount > 0) {
                reward += "，回复 " + healAmount + " 生命值";
            }

            System.out.println("[CombatEngine] 战斗胜利！" + reward);

            if (viewNotifier != null) {
                viewNotifier.onBattleEnd(true, reward);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取所有存活的敌人（作为 AbstractEntity 列表）。
     *
     * @return 存活实体列表
     */
    private List<AbstractEntity> getAliveEnemies() {
        List<AbstractEntity> alive = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                alive.add(enemy);
            }
        }
        return alive;
    }

    /**
     * 获取所有存活的敌人（作为 Enemy 列表）。
     *
     * @return 存活敌人列表
     */
    public List<Enemy> getAliveEnemyList() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                alive.add(enemy);
            }
        }
        return alive;
    }

    // ==================== Getter ====================

    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
    public int getTurn() { return turn; }
    public boolean isInBattle() { return inBattle; }
    public boolean isVictory() { return victory; }
    public ActionManager getActionManager() { return actionManager; }

    public void setBattleNodeType(NodeType battleNodeType) {
        this.battleNodeType = battleNodeType != null ? battleNodeType : NodeType.FIGHT;
    }

    public void setRelicManager(RelicManager relicManager) {
        this.relicManager = relicManager;
    }

    private void queueAction(CombatAction action) {
        if (action instanceof DamageAction damageAction && relicManager != null) {
            damageAction.setDamageModifier(relicManager::modifyDamage);
        }
        if (action instanceof HealAction healAction && relicManager != null) {
            healAction.setHealModifier(relicManager::modifyHeal);
        }
        actionManager.push(action);
    }

    private int calculateGoldReward() {
        int gold = randomBetween(15, 30);
        if (battleNodeType == NodeType.EMERGENCY) {
            gold += randomBetween(20, 30);
        }
        if (player.getCurrentFloor() == 2) {
            gold += randomBetween(10, 15);
        }
        return gold;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private boolean removeCardInstanceFromHand(Card card) {
        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i) == card) {
                hand.remove(i);
                return true;
            }
        }
        return hand.remove(card);
    }
}
