package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.*;
import com.fabricatedbook.core.buff.ExtraEnergyBuff;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.buff.Poison;
import com.fabricatedbook.core.buff.UndeadBuff;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.event.OnCombatStart;
import com.fabricatedbook.core.event.OnEntityDeath;
import com.fabricatedbook.core.event.OnTurnEnd;
import com.fabricatedbook.core.event.OnCardUsed;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.core.relic.EventBus;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Long runSeed;
    private final String randomKey;

    /** 事件总线 */
    private EventBus eventBus;

    /** 当前战斗来源节点类型，用于结算正式奖励区间。 */
    private NodeType battleNodeType;

    /** Enemy intent values that must be shown and later executed identically. */
    private final Map<Enemy, PlannedEnemyAction> plannedEnemyActions;

    /**
     * 构造战斗引擎。
     */
    public CombatEngine() {
        this(new Random());
    }

    public CombatEngine(Random random) {
        this(random, null, null);
    }

    public CombatEngine(long runSeed, String randomKey) {
        this(GameRunState.randomFor(runSeed, randomKey + ":combat-flow"),
                runSeed, randomKey);
    }

    private CombatEngine(Random random, Long runSeed, String randomKey) {
        this.actionManager = new ActionManager();
        this.random = random == null ? new Random() : random;
        this.runSeed = runSeed;
        this.randomKey = randomKey;
        this.turn = 0;
        this.inBattle = false;
        this.victory = false;
        this.eventBus = EventBus.getInstance();
        this.battleNodeType = NodeType.FIGHT;
        this.plannedEnemyActions = new HashMap<>();
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
        this.plannedEnemyActions.clear();
        this.turn = 0;
        this.inBattle = true;
        this.victory = false;

        player.resetForCombatStart();
        player.setShuffleRandom(randomFor("reshuffle"));
        installStatusDamageModifiers();

        // 初始化玩家手牌：抽牌堆放入基础卡牌，抽初始手牌
        initPlayerDeck();

        // 藏品管理器事件订阅
        if (relicManager != null) {
            relicManager.onCombatStart();
            relicManager.modifyEnemiesAtCombatStart(this.enemies);
        }

        applyEnemyCombatStartPassives();

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
                if (atk != null) starterDeck.add(CardFactory.createFromTemplate(atk));
            }
            // 基础防御牌 ×5
            for (int i = 0; i < 5; i++) {
                Card def = CardPool.findById("war_def1");
                if (def != null) starterDeck.add(CardFactory.createFromTemplate(def));
            }
            player.getDrawPile().addAll(starterDeck);
        }
        Collections.shuffle(player.getDrawPile(), randomFor("initial-shuffle"));
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

        // 1. 重置基础能量。ExtraEnergyBuff 在 onTurnStart 中补充额外能量。
        player.setEnergy(player.getMaxEnergy());

        // 2. 清理非装甲格挡（只有装甲Buff的保留格挡）
        // 简化：掉线时不清除格挡并交由 ArmorBuff.onTurnEnd 控制
        // 默认清除所有格挡，装甲Buff会阻止
        boolean hasArmor = player.hasBuff("ArmorBuff");
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

        // 4. 调用已有 BuffHook.onTurnStart
        for (BuffHook buff : player.getBuffs()) {
            buff.onTurnStart(player);
        }
        for (Enemy enemy : new ArrayList<>(enemies)) {
            for (BuffHook buff : enemy.getBuffs()) {
                buff.onTurnStart(enemy);
            }
        }

        // 5. 清理过期 Buff
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }

        // 回合开始型藏品和怪物被动在已有 Buff 结算后触发，
        // 避免本回合新施加的中毒/凋零立刻被同一轮 Buff tick 消耗。
        if (relicManager != null) {
            relicManager.onTurnStart(getAliveEnemyList());
        }
        applyEnemyTurnStartPassives();

        // 6. 抽牌
        player.drawCards(drawCount);

        // 7. 通知视图
        if (viewNotifier != null) {
            viewNotifier.onTurnStart(turn);
        }

        // 8. 检查敌人意图
        plannedEnemyActions.clear();
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                String actionId = enemy.peekCurrentAction();
                String materializedActionId = materializeIntentAction(actionId);
                if (!materializedActionId.equals(actionId)) {
                    plannedEnemyActions.put(enemy,
                            new PlannedEnemyAction(actionId, materializedActionId));
                }
                enemy.deduceIntent(materializedActionId);
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

        int energyCost = energyCostFor(card);

        // 检查能量
        if (player.getEnergy() < energyCost) {
            System.out.println("[CombatEngine] 能量不足，需要 " + card.getCost()
                    + "，当前 " + player.getEnergy());
            return false;
        }

        // 从手牌移除。手牌可能有多张同 ID 基础牌，必须按实例移除。
        if (!removeCardInstanceFromHand(card)) {
            return false;
        }

        // 消耗能量
        player.spendEnergy(energyCost);

        // 解析卡牌效果
        List<CombatAction> actions = parseCardEffects(card, target, energyCost);

        // 将动作加入队列
        for (CombatAction action : actions) {
            queueAction(action);
        }

        // 处理消耗
        if (card.isExhaust()) {
            player.getExhaustPile().add(card);
            System.out.println("[CombatEngine] 消耗牌: " + card.getName());
        } else {
            player.getDiscardPile().add(card);
        }

        // 执行队列
        executeActionQueue();

        if (relicManager != null) {
            relicManager.fireCardUsed(card, energyCost);
        }

        // 通知视图
        if (viewNotifier != null && !actions.isEmpty()) {
            viewNotifier.onCardPlayed(actions.get(0));
        }

        // 检查战斗状态
        checkBattleEnd();

        return true;
    }

    private int energyCostFor(Card card) {
        return card.getCost() < 0 ? player.getEnergy() : card.getCost();
    }

    private void addBaseDamageToLastDamageAction(List<CombatAction> actions, int amount) {
        if (amount <= 0) return;
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof DamageAction damageAction) {
                actions.set(i, damageAction.withAddedBaseDamage(amount));
                return;
            }
        }
    }

    /**
     * 解析卡牌效果生成 CombatAction 列表。
     * <p>
     * 效果字符串格式说明：
     * - "damage:N" -> 对单目标造成 N 点伤害
     * - "damage_all:N" -> 对所有敌人造成 N 点伤害
     * - "damage_x:N" -> 对单目标造成 X 段 N 点伤害
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
    private List<CombatAction> parseCardEffects(Card card, Enemy target, int energySpent) {
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
                case "damage_x": {
                    int dmg = Integer.parseInt(parts[1]);
                    for (int i = 0; i < energySpent; i++) {
                        List<AbstractEntity> singleTarget = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            singleTarget.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            singleTarget.add(aliveEnemies.get(0));
                        }
                        if (!singleTarget.isEmpty()) {
                            actions.add(new DamageAction(player, singleTarget, dmg, true));
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
                    if (isExtraEnergyBuff(buffName) && parts.length > 4) {
                        int extraEnergyPerTurn = Integer.parseInt(parts[4]);
                        actions.add(new ApplyBuffAction(player,
                                s -> new ExtraEnergyBuff(s, extraEnergyPerTurn), stack));
                    } else {
                        actions.add(new ApplyBuffAction(player, buffName, stack));
                    }
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
                    addBaseDamageToLastDamageAction(actions, bonus * attackCount);
                    break;
                }
                case "bonus_low_hp": {
                    int threshold = Integer.parseInt(parts[1]);
                    int bonus = Integer.parseInt(parts[2]);
                    if (target != null && target.getHp() < threshold && target.isAlive()) {
                        addBaseDamageToLastDamageAction(actions, bonus);
                    }
                    break;
                }
                case "detonate_withering": {
                    int times = Integer.parseInt(parts[1]);
                    if (target != null && target.isAlive()) {
                        actions.add(new TriggerWitheringAction(target, times));
                    }
                    break;
                }
                case "double_poison": {
                    actions.add(new DoublePoisonAction(new ArrayList<>(aliveEnemies)));
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
                        addBaseDamageToLastDamageAction(actions, extraDmg);
                    }
                    break;
                }
                case "add_random_attack": {
                    List<Card> attackCards = CardPool.getCardsByProfession(player.getProfession().name().toLowerCase())
                            .stream().filter(c -> c.getType() == Card.CardType.ATTACK).toList();
                    if (!attackCards.isEmpty()) {
                        Card randomCard = attackCards.get(random.nextInt(attackCards.size()));
                        if (randomCard != null) {
                            player.getHand().add(CardFactory.createFromTemplate(randomCard));
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
                        addBaseDamageToLastDamageAction(actions,
                                card.getEscalatingBonus() - bonus); // 本次之前已累计的额外伤害
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
                case "poison_chance": {
                    // poison_chance:Chance:Stack
                    int chance = Integer.parseInt(parts[1]);
                    int stack = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    if (random.nextInt(100) < chance) {
                        List<AbstractEntity> poisonTargets = new ArrayList<>();
                        if (target != null && target.isAlive()) {
                            poisonTargets.add(target);
                        } else if (!aliveEnemies.isEmpty()) {
                            poisonTargets.add(aliveEnemies.get(0));
                        }
                        if (!poisonTargets.isEmpty()) {
                            actions.add(new ApplyBuffAction(poisonTargets.get(0), "Poison", stack));
                        }
                    }
                    break;
                }
                case "trigger_withering": {
                    // trigger_withering 或 trigger_withering:N — 引爆凋零
                    int times = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (target != null && target.isAlive()) {
                        actions.add(new TriggerWitheringAction(target, times));
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

    private boolean isExtraEnergyBuff(String buffName) {
        return "extra_energy".equalsIgnoreCase(buffName)
                || "extraenergy".equalsIgnoreCase(buffName)
                || "ExtraEnergyBuff".equalsIgnoreCase(buffName);
    }

    /**
     * 执行动作队列中的所有动作。
     */
    private void executeActionQueue() {
        while (!actionManager.isAllFinished()) {
            CombatAction action = actionManager.executeNext();
            if (action instanceof DamageAction damageAction) {
                processEnemyAttackPassives(damageAction);
                applyEnemyDeathPassives();
            }
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
            if (buff instanceof UndeadBuff undeadBuff) {
                undeadBuff.tick(player);
            }
        }
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                for (BuffHook buff : enemy.getBuffs()) {
                    buff.onTurnEnd(enemy);
                }
            }
        }

        // 2. 处理回合结束手牌：虚无进消耗堆，保留留手，其他弃牌。
        player.resolveEndOfTurnHand();

        // 3. 清理过期 Buff
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }

        // 玩家中毒在玩家回合结束、敌方回合开始前结算；该伤害可被格挡抵消。
        // 敌方中毒也在敌方行动前结算；该伤害穿透格挡，敌人可能因此无法行动。
        resolvePoisonBeforeEnemyTurn();
        player.cleanExpiredBuffs();
        for (Enemy enemy : enemies) {
            enemy.cleanExpiredBuffs();
        }
        applyEnemyDeathPassives();
        checkBattleEnd();
        if (!inBattle) return;

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

    private void resolvePoisonBeforeEnemyTurn() {
        tickPoison(player, true);
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                tickPoison(enemy, false);
            }
        }
    }

    private void tickPoison(AbstractEntity target, boolean blockedByBlock) {
        for (BuffHook buff : new ArrayList<>(target.getBuffs())) {
            if (buff instanceof Poison poison) {
                poison.tick(target, blockedByBlock);
                return;
            }
        }
    }

    /**
     * 执行所有存活敌人的行动。
     */
    private void executeEnemyTurns() {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            String originalActionId = enemy.getCurrentAction();
            String actionId = consumePlannedAction(enemy, originalActionId);
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
                relicManager.onCombatVictory();
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

            int healAmount = finalBoss ? 0 : player.battleRewardHeal(randomFor("victory-heal"));

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

    public String getPreviewActionId(Enemy enemy) {
        if (enemy == null) return null;
        PlannedEnemyAction planned = plannedEnemyActions.get(enemy);
        if (planned != null) {
            return planned.materializedActionId;
        }
        return enemy.peekCurrentAction();
    }

    public void setBattleNodeType(NodeType battleNodeType) {
        this.battleNodeType = battleNodeType != null ? battleNodeType : NodeType.FIGHT;
    }

    public void setRelicManager(RelicManager relicManager) {
        this.relicManager = relicManager;
    }

    private void queueAction(CombatAction action) {
        if (action instanceof DamageAction damageAction) {
            damageAction.setDamageModifier((damage, source, target) -> {
                int modified = relicManager != null
                        ? relicManager.modifyDamage(damage, source, target)
                        : damage;
                return modifyEnemyPassiveDamage(modified, source, target);
            });
        }
        if (action instanceof HealAction healAction && relicManager != null) {
            healAction.setHealModifier(relicManager::modifyHeal);
        }
        actionManager.push(action);
    }

    private int calculateGoldReward() {
        Random rewardRandom = randomFor("gold-reward");
        int gold = randomBetween(15, 30, rewardRandom);
        if (battleNodeType == NodeType.EMERGENCY) {
            gold += randomBetween(20, 30, rewardRandom);
        }
        if (player.getCurrentFloor() == 2) {
            gold += randomBetween(10, 15, rewardRandom);
        }
        return gold;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private int randomBetween(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }

    private Random randomFor(String purpose) {
        if (runSeed == null || randomKey == null) {
            return random;
        }
        return GameRunState.randomFor(runSeed, randomKey + ":" + purpose);
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

    private void installStatusDamageModifiers() {
        if (relicManager == null) return;
        player.setStatusDamageModifier(relicManager::modifyStatusDamage);
        for (Enemy enemy : enemies) {
            enemy.setStatusDamageModifier(relicManager::modifyStatusDamage);
        }
    }

    private void applyEnemyCombatStartPassives() {
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) continue;
            switch (enemy.getPassive()) {
                case "self_fragile_passive" ->
                        queueAction(new ApplyBuffAction(enemy, "Fragile", 99));
                case "start_block_35" ->
                        queueAction(new GainBlockAction(enemy, 35));
                case "spawn_puppet" -> ensurePuppetExists();
                case "summon_turrets" -> ensureSentinelTurretsExist();
                default -> {
                }
            }
        }
        executeActionQueue();
    }

    private void applyEnemyTurnStartPassives() {
        for (Enemy enemy : new ArrayList<>(enemies)) {
            if (enemy == null || !enemy.isAlive()) continue;
            switch (enemy.getPassive()) {
                case "def_bark_3" -> queueAction(new GainBlockAction(enemy, 3));
                case "buff_strength_turns" -> {
                    if (turn <= 3) {
                        queueAction(new ApplyBuffAction(enemy, "Strength", 1));
                    } else {
                        queueAction(new ApplyBuffAction(enemy, "Weak", 1));
                    }
                }
                case "buff_strength_5_turns" -> {
                    if (turn <= 5) {
                        queueAction(new ApplyBuffAction(enemy, "Strength", 1));
                    } else {
                        queueAction(new ApplyBuffAction(enemy, "Weak", 1));
                    }
                }
                case "regen_5", "regen_on_attack" -> queueAction(new HealAction(enemy, 5));
                case "poison_player_turn_start" ->
                        queueAction(new ApplyBuffAction(player, "Poison", 1));
                case "block_reduction_player" ->
                        queueAction(new ApplyBuffAction(player, "BlockReduction", 1));
                case "self_damage_passive" ->
                        enemy.setHp(enemy.getHp() - randomBetween(20, 40));
                case "strength_after_5_turns" -> {
                    if (turn >= 5 && enemy.markPassiveTriggered("strength_after_5_turns")) {
                        queueAction(new ApplyBuffAction(enemy, "Strength", 3));
                    }
                }
                case "summon_turrets" -> {
                    if (enemy.getHp() * 2 < enemy.getMaxHp()
                            && enemy.markPassiveTriggered("sentinel_half_hp_strength")) {
                        for (Enemy ally : getAliveEnemyList()) {
                            queueAction(new ApplyBuffAction(ally, "Strength", 2));
                        }
                    }
                }
                case "buff_master_random" -> buffPuppetMaster();
                default -> {
                }
            }
        }
        executeActionQueue();
        applyEnemyDeathPassives();
    }

    private int modifyEnemyPassiveDamage(int damage, AbstractEntity source,
                                         AbstractEntity target) {
        if (source instanceof Enemy enemy
                && "boost_damage_passive".equals(enemy.getPassive())
                && random.nextInt(100) < 20) {
            return damage + Math.max(1, damage / 5);
        }
        return damage;
    }

    private void processEnemyAttackPassives(DamageAction action) {
        if (!(action.getSource() instanceof Enemy enemy) || !enemy.isAlive()) {
            return;
        }
        if (action.getFinalDamageByTarget().isEmpty()) {
            return;
        }
        for (AbstractEntity target : action.getFinalDamageByTarget().keySet()) {
            switch (enemy.getPassive()) {
                case "wither_passive" -> {
                    if (random.nextInt(100) < 25) {
                        if (random.nextBoolean()) {
                            queueAction(new TriggerWitheringAction(target, 1));
                        } else {
                            queueAction(new ApplyBuffAction(target, "Withering", 1));
                        }
                    }
                }
                case "poison_on_attack" ->
                        queueAction(new ApplyBuffAction(target, "Poison", 1));
                case "self_damage_passive" ->
                        queueAction(new ApplyBuffAction(target, "Withering", 1));
                case "regen_on_attack" ->
                        queueAction(new HealAction(enemy, 3));
                case "bonus_damage_passive" -> {
                    if (random.nextInt(100) < 25) {
                        target.takeDamage(3);
                    }
                }
                case "wither_on_attack" ->
                        queueAction(new TriggerWitheringAction(target, 1));
                case "wither_poison_on_attack" -> {
                    queueAction(new TriggerWitheringAction(target, 1));
                    queueAction(new ApplyBuffAction(target, "Poison", 2));
                }
                default -> {
                }
            }
        }
    }

    private void applyEnemyDeathPassives() {
        boolean sentinelAlive = enemies.stream()
                .anyMatch(e -> e != null && e.isAlive()
                        && "summon_turrets".equals(e.getPassive()));
        if (sentinelAlive) return;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()
                    && "self_destruct".equals(enemy.getPassive())) {
                enemy.setHp(0);
                enemy.setAlive(false);
            }
        }
    }

    private void buffPuppetMaster() {
        for (Enemy ally : enemies) {
            if (ally != null && ally.isAlive() && "spawn_puppet".equals(ally.getPassive())) {
                String[] buffs = {"Strength", "Resistance", "BlockIncrease"};
                queueAction(new ApplyBuffAction(ally, buffs[random.nextInt(buffs.length)], 3));
                return;
            }
        }
    }

    private void ensurePuppetExists() {
        boolean hasPuppet = enemies.stream().anyMatch(e -> e != null
                && "buff_master_random".equals(e.getPassive()));
        if (!hasPuppet) {
            Enemy puppet = new Enemy("puppet", "傀儡", 50,
                    List.of("sacrifice", "trigger_wither_puppet", "def_shield_puppet"),
                    "buff_master_random");
            puppet.setStatusDamageModifier(relicManager != null
                    ? relicManager::modifyStatusDamage : null);
            enemies.add(puppet);
        }
    }

    private void ensureSentinelTurretsExist() {
        long turretCount = enemies.stream()
                .filter(e -> e != null && "self_destruct".equals(e.getPassive()))
                .count();
        for (long i = turretCount; i < 2; i++) {
            Enemy turret = new Enemy("sentinel_turret_" + (i + 1), "炮塔", 20,
                    List.of("atk_shot", "atk_overload_shot"), "self_destruct");
            turret.setStatusDamageModifier(relicManager != null
                    ? relicManager::modifyStatusDamage : null);
            enemies.add(turret);
        }
    }

    private String materializeIntentAction(String actionId) {
        if (actionId == null) return null;
        return switch (actionId) {
            case "atk_berserk" -> "atk" + randomBetween(12, 20);
            case "atk_chaos" -> "atk" + randomBetween(13, 20);
            case "def_chaos" -> "def" + randomBetween(8, 20);
            default -> actionId;
        };
    }

    private String consumePlannedAction(Enemy enemy, String originalActionId) {
        PlannedEnemyAction planned = plannedEnemyActions.remove(enemy);
        if (planned == null) {
            return originalActionId;
        }
        return planned.originalActionId.equals(originalActionId)
                ? planned.materializedActionId
                : originalActionId;
    }

    private static class PlannedEnemyAction {
        private final String originalActionId;
        private final String materializedActionId;

        private PlannedEnemyAction(String originalActionId, String materializedActionId) {
            this.originalActionId = originalActionId;
            this.materializedActionId = materializedActionId;
        }
    }
}
