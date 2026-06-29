package com.fabricatedbook.desktop;

import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.engine.CardEffect;
import com.fabricatedbook.core.engine.CardEffectParser;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.event.EventResultResolver;
import com.fabricatedbook.core.event.EventRewardResolver;
import com.fabricatedbook.core.map.LayerMapConfig;
import com.fabricatedbook.core.map.LayerMapGraph;
import com.fabricatedbook.core.map.LayerMapNode;
import com.fabricatedbook.core.map.NodeEntryResolver;
import com.fabricatedbook.core.map.NodeEntryResult;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicData;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.data.DataLoader;
import com.fabricatedbook.data.SaveManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * BackendDebugLauncher — 后端命令行调试入口。
 * <p>
 * 不启动 LibGDX 前端，直接调试地图生成、路线选择和战斗交互。
 */
public class BackendDebugLauncher {

    private final Scanner scanner = new Scanner(System.in);
    private Random random = new Random();
    private final SaveManager saveManager = new SaveManager();
    private final List<LayerMapConfig> configs = new DataLoader().loadLayerMapConfigs();

    private GameRunState runState;
    private Player player;
    private LayerMapGraph map;
    private int levelIndex;
    private boolean running = true;

    public static void main(String[] args) {
        new BackendDebugLauncher().run(args);
    }

    private void run(String[] args) {
        long seed = parseSeedArg(args, System.currentTimeMillis());
        startNewRun(seed);

        println("Fabricated Book 后端调试控制台");
        println("输入 help 查看命令。");
        printStatus();
        printMap();

        while (running) {
            print("> ");
            String line = readLine();
            if (line == null) {
                running = false;
                println("");
                println("输入结束，已退出。");
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            handleMapCommand(line);
        }
    }

    private void handleMapCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help":
                printHelp();
                break;
            case "status":
                printStatus();
                break;
            case "seed":
                printSeed();
                break;
            case "map":
                printMap();
                break;
            case "routes":
                printRoutes();
                break;
            case "choose":
            case "go":
                if (parts.length < 2) {
                    println("用法: choose <路线编号>");
                } else {
                    chooseRoute(parseInt(parts[1], -1));
                }
                break;
            case "battle":
                runBattle(NodeType.FIGHT);
                break;
            case "save":
                saveRun();
                break;
            case "load":
            case "continue":
                loadRun();
                break;
            case "newrun":
                startNewRun(parts.length >= 2 ? parseLong(parts[1], System.currentTimeMillis())
                        : System.currentTimeMillis());
                printStatus();
                printMap();
                break;
            case "deck":
                printDeck();
                break;
            case "potions":
                printPotions();
                break;
            case "relics":
                printRelics();
                break;
            case "givepotion":
                givePotion(parts.length >= 2 ? parts[1] : "");
                break;
            case "giverelic":
                giveRelic(parts.length >= 2 ? parts[1] : "");
                break;
            case "selftest":
                runSelfTest();
                break;
            case "seedtest":
                runSeedTest(parts.length >= 2 ? parseLong(parts[1], runState.getSeed())
                        : runState.getSeed());
                break;
            case "savetest":
                runSaveTest();
                break;
            case "newmap":
                createMap();
                printMap();
                break;
            case "quit":
            case "exit":
                running = false;
                println("已退出。");
                break;
            default:
                println("未知命令: " + command + "，输入 help 查看命令。");
                break;
        }
    }

    private void printHelp() {
        println("");
        println("地图命令:");
        println("  status             查看玩家状态");
        println("  seed               查看当前对局随机种子");
        println("  map                打印当前层地图");
        println("  routes             查看当前位置可选路线");
        println("  choose <编号>       选择一条可达路线并进入节点");
        println("  battle             直接启动一场普通战斗");
        println("  save               保存当前对局");
        println("  load/continue      从存档继续对局");
        println("  newrun [seed]      用指定/随机种子开始新对局");
        println("  deck               查看当前牌堆/手牌/弃牌堆数量");
        println("  potions            查看药水栏");
        println("  relics             查看藏品列表");
        println("  givepotion <id>    获得一瓶药水，random 随机");
        println("  giverelic <id>     获得一个藏品，random 随机");
        println("  selftest           运行数据/卡牌/怪物/药水/藏品自检");
        println("  seedtest [seed]    验证同种子地图和战斗抽牌顺序可复现");
        println("  savetest           验证战斗中存档回到战斗前快照");
        println("  newmap             重新生成当前层地图");
        println("  quit               退出调试控制台");
        println("");
        println("战斗命令会在进入战斗后显示。");
    }

    private void startNewRun(long seed) {
        player = new Player("debug-player", "调试战士", Profession.WARRIOR);
        player.setGold(80);
        levelIndex = 0;
        runState = new GameRunState(seed, player);
        random = runState.randomFor("backend-cli");
        createMap();
    }

    private void createMap() {
        LayerMapConfig config = configs.get(levelIndex);
        runState.setCurrentLayerIdx(levelIndex);
        map = new LayerMapGraph(config, runState.getSeed(), levelIndex);
        player.setCurrentFloor(config.getLevel());
    }

    private void printStatus() {
        println("玩家: " + player.getName()
                + " HP " + player.getHp() + "/" + player.getMaxHp()
                + " 金币 " + player.getGold()
                + " 当前层 " + configs.get(levelIndex).getLevelName()
                + " 种子 " + runState.getSeed());
    }

    private void printSeed() {
        println("当前对局种子: " + runState.getSeed());
    }

    private void saveRun() {
        if (saveManager.saveRun(runState)) {
            println("已保存对局。");
        } else {
            println("保存失败。");
        }
    }

    private void loadRun() {
        GameRunState loaded = saveManager.loadRun();
        if (loaded == null) {
            println("读档失败或无存档。");
            return;
        }
        runState = loaded;
        player = loaded.getPlayer();
        levelIndex = loaded.getCurrentLayerIdx();
        random = runState.randomFor("backend-cli");
        createMap();
        restoreCompletedNode(loaded.getCompletedNode());
        println("已继续对局。");
        printStatus();
        printMap();
    }

    private void restoreCompletedNode(GameRunState.NodeRef nodeRef) {
        if (nodeRef == null) {
            return;
        }
        levelIndex = Math.max(0, Math.min(configs.size() - 1, nodeRef.layer));
        runState.setCurrentLayerIdx(levelIndex);
        createMap();
        map.restorePosition(nodeRef.col, nodeRef.row);
    }

    private GameRunState.NodeRef toNodeRef(LayerMapNode node) {
        if (node == null) return null;
        return new GameRunState.NodeRef(levelIndex, node.getCol(), node.getRow(),
                node.getTypeCode());
    }

    private Random randomForNode(String purpose, LayerMapNode node) {
        if (node == null) {
            return runState.randomFor(purpose, levelIndex, "none");
        }
        return runState.randomFor(purpose, levelIndex, node.getCol(), node.getRow(),
                node.getType().name());
    }

    private void printMap() {
        LayerMapConfig config = map.getConfig();
        println("");
        println("地图: " + config.getLevelName()
                + " (" + config.getLength() + "列, 最大" + config.getWidth() + "行)"
                + " 环境: " + config.getEffectText());
        for (LayerMapNode[] column : map.getColumns()) {
            StringBuilder builder = new StringBuilder();
            for (LayerMapNode node : column) {
                String marker = " ";
                if (node == map.getCurrentNode()) {
                    marker = "@";
                } else if (node.isVisited()) {
                    marker = "*";
                } else if (map.getAvailableNodes().contains(node)) {
                    marker = "!";
                }
                builder.append(String.format("[%s%d,%d %-4s] ",
                        marker,
                        node.getCol(),
                        node.getRow(),
                        shortName(node.getType())));
            }
            println(builder.toString());
        }
        printRoutes();
    }

    private void printRoutes() {
        List<LayerMapNode> available = map.getAvailableNodes();
        if (available.isEmpty()) {
            println("当前没有可选路线。");
            return;
        }
        println("可选路线:");
        for (int i = 0; i < available.size(); i++) {
            LayerMapNode node = available.get(i);
            println("  " + (i + 1) + ". " + node.getType().getDisplayName()
                    + " (" + node.getCol() + "," + node.getRow() + ")");
        }
    }

    private void chooseRoute(int index) {
        List<LayerMapNode> available = map.getAvailableNodes();
        if (index < 1 || index > available.size()) {
            println("路线编号无效。");
            printRoutes();
            return;
        }

        LayerMapNode node = available.get(index - 1);
        if (!map.moveTo(node)) {
            println("移动失败: " + node);
            return;
        }

        println("进入节点: " + node.getType().getDisplayName()
                + " (" + node.getCol() + "," + node.getRow() + ")");
        runState.beginNode(toNodeRef(node));
        NodeEntryResult entryResult = new NodeEntryResolver().enterNode(runState, toNodeRef(node));
        for (String message : entryResult.getMessages()) {
            println(message);
        }
        resolveNode(node);
        if (node.getType().isCombat()) {
            if (player.isAlive()) {
                runState.completeActiveNode();
            }
        } else {
            runState.completeActiveNode();
        }

        if (!running) {
            return;
        }
        if (map.isAtEnd()) {
            advanceLevel();
        }
        printStatus();
        printMap();
    }

    private void resolveNode(LayerMapNode node) {
        NodeType type = node.getType();
        if (type.isCombat()) {
            runBattle(type);
        } else if (type == NodeType.SHOP) {
            int gain = 15;
            player.gainGold(gain);
            println("商店调试: 暂未进入购买流程，补偿获得 " + gain + " 金币。");
        } else if (type == NodeType.REWARD) {
            int gain = 25;
            player.gainGold(gain);
            println("奖励调试: 获得 " + gain + " 金币。");
        } else if (type == NodeType.SAFEHOUSE) {
            int healed = player.heal(12);
            println("安全屋: 回复 " + healed + " 生命值。");
        } else if (type == NodeType.DECISION) {
            resolveDecision();
        } else {
            resolveEvent();
        }
    }

    private void resolveDecision() {
        println("命运抉择: 迷雾挡住了来路。");
        println("  1. 前进 - 突破迷雾，继续旅程");
        println("  2. 回头 - 结束游戏，获得隐藏结局「讲述中断」");
        println("输入抉择编号，或回车选择 1。");
        print("decision> ");
        String line = readLine();
        if (line == null) {
            line = "";
        }
        int choice = line.trim().isEmpty() ? 1 : parseInt(line.trim(), 1);
        if (choice == 2) {
            println("隐藏结局: 讲述中断。");
            running = false;
        } else {
            println("你选择继续前进。");
        }
    }

    private void resolveEvent() {
        Random eventRandom = randomForNode("event-result", map.getCurrentNode());
        EventHandler handler = new EventHandler(eventRandom);
        List<String> names = handler.getEventNames();
        String eventName = names.get(randomForNode("event-name", map.getCurrentNode())
                .nextInt(names.size()));
        List<EventHandler.EventOption> options = handler.getOptions(eventName);
        println("事件: " + eventName);
        for (int i = 0; i < options.size(); i++) {
            EventHandler.EventOption option = options.get(i);
            println("  " + (i + 1) + ". " + option.label + " - " + option.description);
        }
        println("输入事件选项编号，或回车选择 1。");
        print("event> ");
        String line = readLine();
        if (line == null) {
            line = "";
        }
        line = line.trim();
        int choice = line.isEmpty() ? 1 : parseInt(line, 1);
        choice = Math.max(1, Math.min(choice, options.size()));

        EventHandler.EventResult result = handler.executeEvent(eventName, choice - 1);
        applyEventResult(result, eventRandom);
        println(result.description);
    }

    private void applyEventResult(EventHandler.EventResult result, Random eventRandom) {
        if (result.goldChange > 0) {
            player.gainGold(result.goldChange);
        } else if (result.goldChange < 0) {
            player.spendGold(-result.goldChange);
        }

        if (result.fullHeal) {
            player.heal(player.getMaxHp());
        } else if (result.hpChange > 0) {
            player.heal(result.hpChange);
        } else if (result.hpChange < 0) {
            player.takeDamage(-result.hpChange);
        }

        if (result.relicId != null && !result.relicId.isBlank()) {
            EventRewardResolver.applyRewards(result, player, eventRandom);
        }
    }

    private void advanceLevel() {
        if (levelIndex >= configs.size() - 1) {
            println("已经到达最后一层终点，后端路线调试完成。");
            running = false;
            return;
        }
        levelIndex++;
        createMap();
        println("进入下一层: " + configs.get(levelIndex).getLevelName());
    }

    private void runBattle(NodeType nodeType) {
        List<Enemy> enemies = createEnemies(nodeType);
        CombatEngine engine = new CombatEngine(runState.getSeed(),
                "backend-combat:" + levelIndex + ":" + nodeType.name());
        engine.setBattleNodeType(nodeType);
        engine.setEnvironmentDamageModifier(runState.getMapDamageModifier());
        engine.setRelicManager(new RelicManager(player));
        engine.setViewNotifier(new ConsoleNotifier());
        engine.initBattle(player, enemies);

        printBattleHelp();
        while (engine.isInBattle() && running) {
            printBattleState(engine);
            print("battle> ");
            String line = readLine();
            if (line == null) {
                running = false;
                println("");
                println("输入结束，已退出。");
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            handleBattleCommand(engine, line);
        }

        player.discardHand();
        if (!player.isAlive()) {
            println("玩家已死亡，调试结束。");
            running = false;
        }
    }

    private void handleBattleCommand(CombatEngine engine, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help":
                printBattleHelp();
                break;
            case "state":
                printBattleState(engine);
                break;
            case "hand":
                printHand(engine.getPlayer());
                break;
            case "enemies":
                printEnemies(engine.getEnemies());
                break;
            case "potions":
                printPotions();
                break;
            case "usepotion":
                usePotion(engine, parts);
                break;
            case "relics":
                printRelics();
                break;
            case "play":
                playCard(engine, parts);
                break;
            case "end":
                engine.endRound();
                break;
            case "auto":
                autoBattle(engine);
                break;
            case "quit":
                running = false;
                break;
            default:
                println("未知战斗命令: " + command);
                printBattleHelp();
                break;
        }
    }

    private void usePotion(CombatEngine engine, String[] parts) {
        if (parts.length < 2) {
            println("用法: usepotion <药水编号>");
            printPotions();
            return;
        }
        int index = parseInt(parts[1], -1) - 1;
        if (index < 0 || index >= player.getPotions().size()) {
            println("药水编号无效。");
            printPotions();
            return;
        }
        Potion potion = player.removePotion(index);
        boolean used = potion != null && potion.use(player, engine.getEnemies(), new RelicManager(player));
        println(used ? "使用药水: " + potion.getName() : "药水使用失败。");
        engine.checkBattleEnd();
    }

    private void playCard(CombatEngine engine, String[] parts) {
        if (parts.length < 2) {
            println("用法: play <手牌编号> [敌人编号]");
            return;
        }
        List<Integer> cardIndexes = parseCardIndexes(parts[1]);
        if (cardIndexes.isEmpty()) {
            println("用法: play <手牌编号或编号列表> [敌人编号]，例如 play 1 或 play 2,3");
            return;
        }

        List<Card> handSnapshot = new ArrayList<>(engine.getPlayer().getHand());
        for (int cardIndex : cardIndexes) {
            if (cardIndex < 0 || cardIndex >= handSnapshot.size()) {
                println("手牌编号无效。");
                printHand(engine.getPlayer());
                return;
            }
        }

        if (engine.getPlayer().getHand().isEmpty()) {
            println("手牌编号无效。");
            printHand(engine.getPlayer());
            return;
        }

        List<Enemy> alive = engine.getAliveEnemyList();
        Enemy target = null;
        if (!alive.isEmpty()) {
            int targetIndex = parts.length >= 3 ? parseInt(parts[2], 1) - 1 : 0;
            if (targetIndex < 0 || targetIndex >= alive.size()) {
                println("敌人编号无效。");
                printEnemies(alive);
                return;
            }
            target = alive.get(targetIndex);
        }

        for (int cardIndex : cardIndexes) {
            Card card = handSnapshot.get(cardIndex);
            if (!handContainsInstance(engine.getPlayer().getHand(), card)) {
                println("出牌失败: " + cardLabel(card, handSnapshot)
                        + " 已不在手牌。");
                continue;
            }
            boolean played = engine.playCard(card, target);
            if (!played) {
                println("出牌失败: " + cardLabel(card, handSnapshot)
                        + "，可能是能量不足或目标无效。");
            }
            if (!engine.isInBattle()) {
                break;
            }
        }
    }

    private void autoBattle(CombatEngine engine) {
        int guard = 0;
        while (engine.isInBattle() && guard++ < 100) {
            boolean playedAny = false;
            List<Card> handSnapshot = new ArrayList<>(engine.getPlayer().getHand());
            for (Card card : handSnapshot) {
                if (!engine.isInBattle()) {
                    break;
                }
                if (engine.getPlayer().getEnergy() >= card.getCost()) {
                    Enemy target = engine.getAliveEnemyList().isEmpty()
                            ? null
                            : engine.getAliveEnemyList().get(0);
                    if (engine.playCard(card, target)) {
                        playedAny = true;
                    }
                }
            }
            if (engine.isInBattle()) {
                engine.endRound();
            }
            if (!playedAny && !engine.isInBattle()) {
                break;
            }
        }
    }

    private List<Enemy> createEnemies(NodeType nodeType) {
        DataLoader loader = new DataLoader();
        int level = configs.get(levelIndex).getLevel();
        List<DataLoader.EnemyGroup> groups = loader.loadMonsters(level);
        List<DataLoader.EnemyGroup> matched = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (matchesNodeType(group, nodeType)) {
                matched.add(group);
            }
        }
        if (matched.isEmpty() && nodeType == NodeType.EMERGENCY) {
            for (DataLoader.EnemyGroup group : groups) {
                if (!group.isBoss() && !isEmergencyGroup(group)) {
                    matched.add(group);
                }
            }
        }
        if (!matched.isEmpty()) {
            Random enemyRandom = runState.randomFor("backend-enemies", levelIndex, nodeType.name());
            DataLoader.EnemyGroup selected = matched.get(enemyRandom.nextInt(matched.size()));
            if (nodeType == NodeType.EMERGENCY) {
                for (int i = 0; i < 3; i++) {
                    DataLoader.EnemyGroup candidate = matched.get(enemyRandom.nextInt(matched.size()));
                    if (totalHp(candidate) > totalHp(selected)) {
                        selected = candidate;
                    }
                }
            }
            List<Enemy> enemies = new ArrayList<>();
            for (DataLoader.EnemyData data : selected.getEnemies()) {
                enemies.add(data.toEnemy());
            }
            if (!enemies.isEmpty()) {
                println("敌人组: " + selected.getName() + " (" + selected.getId() + ")");
                return enemies;
            }
        }

        List<Enemy> fallback = new ArrayList<>();
        switch (nodeType) {
            case BOSS -> fallback.add(new Enemy("debug_boss", "命令行首领", 70,
                    List.of("atk8", "def8", "atk12")));
            case EMERGENCY -> fallback.add(new Enemy("debug_elite", "命令行精英", 48,
                    List.of("atk7", "atk5x2", "def10")));
            default -> fallback.add(EntityFactory.createSimpleEnemy("debug_dummy", "命令行假人", 36));
        }
        println("未找到 JSON 敌人组，使用 fallback 调试敌人。");
        return fallback;
    }

    private boolean matchesNodeType(DataLoader.EnemyGroup group, NodeType nodeType) {
        boolean emergency = isEmergencyGroup(group);
        if (nodeType == NodeType.BOSS) {
            return group.isBoss();
        }
        if (nodeType == NodeType.EMERGENCY) {
            return !group.isBoss() && emergency;
        }
        return !group.isBoss() && !emergency;
    }

    private boolean isEmergencyGroup(DataLoader.EnemyGroup group) {
        String id = group.getId() == null ? "" : group.getId().toLowerCase();
        String name = group.getName() == null ? "" : group.getName();
        return id.contains("emergency") || name.contains("紧急");
    }

    private static int totalHp(DataLoader.EnemyGroup group) {
        int total = 0;
        if (group.getEnemies() != null) {
            for (DataLoader.EnemyData data : group.getEnemies()) {
                total += data.getMaxHp();
            }
        }
        return total;
    }

    private void printBattleHelp() {
        println("");
        println("战斗命令:");
        println("  state              查看战斗状态");
        println("  hand               查看手牌");
        println("  enemies            查看敌人");
        println("  potions            查看药水栏");
        println("  usepotion <编号>    使用药水");
        println("  relics             查看藏品列表");
        println("  play <牌> [敌人]    使用手牌，编号从 1 开始；多张牌用逗号分隔");
        println("  end                结束当前回合");
        println("  auto               自动打完整场战斗");
        println("  quit               退出调试控制台");
    }

    private void printBattleState(CombatEngine engine) {
        Player p = engine.getPlayer();
        println("");
        println("玩家 HP " + p.getHp() + "/" + p.getMaxHp()
                + " 格挡 " + p.getBlock()
                + " 能量 " + p.getEnergy()
                + " 回合 " + engine.getTurn());
        printEnemies(engine.getEnemies());
        printHand(p);
    }

    private void printEnemies(List<Enemy> enemies) {
        println("敌人:");
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            println("  " + (i + 1) + ". " + enemy.getName()
                    + " HP " + enemy.getHp() + "/" + enemy.getMaxHp()
                    + " 格挡 " + enemy.getBlock()
                    + " 意图 " + enemy.getIntent()
                    + (enemy.isAlive() ? "" : " [死亡]"));
        }
    }

    private void printHand(Player p) {
        println("手牌:");
        if (p.getHand().isEmpty()) {
            println("  (空)");
            return;
        }
        for (int i = 0; i < p.getHand().size(); i++) {
            Card card = p.getHand().get(i);
            println("  " + (i + 1) + ". [" + card.getCost() + "] "
                    + cardLabel(card, p.getHand()) + " - " + card.getDescription());
        }
    }

    private void printDeck() {
        println("牌堆: 抽牌堆 " + player.getDrawPile().size()
                + " / 手牌 " + player.getHand().size()
                + " / 弃牌堆 " + player.getDiscardPile().size());
    }

    private void printPotions() {
        println("药水: " + player.getPotions().size() + "/"
                + player.getMaxPotionSlots());
        if (player.getPotions().isEmpty()) {
            println("  (空)");
            return;
        }
        for (int i = 0; i < player.getPotions().size(); i++) {
            Potion potion = player.getPotions().get(i);
            println("  " + (i + 1) + ". " + potion.getName()
                    + " - " + potion.getDescription());
        }
    }

    private void printRelics() {
        println("藏品: " + player.getRelics().size());
        if (player.getRelics().isEmpty()) {
            println("  (空)");
            return;
        }
        for (Relic relic : player.getRelics()) {
            println("  - " + relic.getName() + " [" + relic.getRarity().getDisplayName()
                    + "] " + relic.getDescription());
        }
    }

    private void givePotion(String id) {
        List<Potion> potions = new DataLoader().loadPotions();
        Potion selected = null;
        if ("random".equalsIgnoreCase(id) && !potions.isEmpty()) {
            selected = potions.get(random.nextInt(potions.size()));
        } else {
            for (Potion potion : potions) {
                if (potion.getId().equals(id) || potion.getName().equals(id)) {
                    selected = potion;
                    break;
                }
            }
        }
        if (selected == null) {
            println("未找到药水: " + id);
            return;
        }
        if (player.addPotion(selected.copy())) {
            println("获得药水: " + selected.getName());
        } else {
            println("药水栏已满。");
        }
    }

    private void giveRelic(String id) {
        Relic relic = "random".equalsIgnoreCase(id)
                ? RelicFactory.randomRelic(player, true)
                : RelicFactory.createById(id, player);
        if (relic == null) {
            println("未找到藏品: " + id);
            return;
        }
        new RelicManager(player).addRelic(relic);
    }

    private void runSelfTest() {
        println("开始后端自检...");
        boolean ok = true;
        DataLoader loader = new DataLoader();

        List<Card> cards = loader.loadCards("warrior");
        ok &= assertCheck(!cards.isEmpty(), "战士 JSON 卡牌可加载: " + cards.size());
        ok &= assertCheck(availableCardEffectsAreKnown(loader),
                "已配置职业 JSON 卡牌 effect 均已接入实战 DSL");
        ok &= assertCheck(configs.size() == 5
                        && configs.get(3).getEndType() == NodeType.DECISION
                        && configs.get(3).getSpecialBossColumn() == 5,
                "稀疏地图 JSON 配置可加载: " + configs.size() + " 层");

        List<Potion> potions = loader.loadPotions();
        ok &= assertCheck(!potions.isEmpty(), "药水可加载: " + potions.size());

        List<RelicData> relics = loader.loadRelicData();
        ok &= assertCheck(!relics.isEmpty(), "藏品可加载: " + relics.size());

        for (int level = 1; level <= 5; level++) {
            List<DataLoader.EnemyGroup> groups = loader.loadMonsters(level);
            ok &= assertCheck(!groups.isEmpty(), "第 " + level + " 层怪物组可加载: " + groups.size());
        }

        EventHandler eventHandler = new EventHandler(new Random(1));
        EventHandler.EventResult fixedEvent = eventHandler.executeEvent("相遇", 0);
        EventHandler.EventResult placeholderRelicEvent = eventHandler.executeEvent("好诗歪诗", 1);
        EventHandler.EventResult decisionEvent = eventHandler.executeEvent("命运抉择2", 2,
                new Player("selftest-decision", "自检战士", Profession.WARRIOR));
        EventHandler.EventResult weightedEvent = eventHandler.executeEvent("投资", 2);
        Player testPlayer = new Player("selftest", "自检战士", Profession.WARRIOR);
        ok &= assertCheck(fixedEventResultsAreValid(loader.loadEvents()),
                "JSON 固定事件结果字段可解析");
        ok &= assertCheck(eventOptionsDeclareExecution(loader.loadEvents()),
                "事件选项均声明 JSON 结果或 Java executor");
        ok &= assertCheck("relic_betrayal".equals(fixedEvent.relicId)
                        && fixedEvent.description.contains("背叛"),
                "固定事件结果可从 JSON 执行");
        ok &= assertCheck(List.of(-100, 0, 150, 200, 1000)
                        .contains(weightedEvent.goldChange),
                "加权随机事件结果可从 JSON 执行");
        ok &= assertCheck("relic_random_leq3".equals(placeholderRelicEvent.relicId),
                "占位藏品事件结果可从 JSON 执行");
        ok &= assertCheck(decisionEvent.relicId == null
                        && decisionEvent.description.contains("没有作出选择"),
                "命运抉择条件按玩家藏品过滤");
        ok &= assertCheck(!eventHandler.getEventNames().contains("命运抉择1")
                        && !eventHandler.getEventNames().contains("命运抉择2")
                        && eventHandler.getOptions("命运抉择1").size() == 2,
                "命运抉择展示数据来自 JSON 且不进入随机事件池");
        Relic resolvedPlaceholderRelic = EventRewardResolver.resolveRelic(
                "relic_random_leq3", testPlayer, new Random(1));
        Relic resolvedCurseRelic = EventRewardResolver.resolveRelic(
                "relic_curse_random", testPlayer, new Random(1));
        ok &= assertCheck(resolvedPlaceholderRelic != null
                        && resolvedPlaceholderRelic.getRarity() != Relic.Rarity.SPECIAL
                        && resolvedPlaceholderRelic.getRarity() != Relic.Rarity.CURSED
                        && resolvedPlaceholderRelic.getRarity().getValue() <= 3,
                "占位低阶藏品可展开为真实藏品");
        ok &= assertCheck(resolvedCurseRelic != null
                        && resolvedCurseRelic.getRarity() == Relic.Rarity.CURSED,
                "占位负面藏品可展开为真实负面藏品");
        int cardCountBeforeEventReward = testPlayer.getDrawPile().size();
        EventHandler.EventResult fiveCardsResult = new EventHandler.EventResult(
                "获得 5 张牌", 0, 0, "relic_five_cards");
        EventRewardResolver.EventReward fiveCardsReward =
                EventRewardResolver.applyRewards(fiveCardsResult, testPlayer,
                        new Random(1));
        ok &= assertCheck(fiveCardsReward.getCards().size() == 5
                        && testPlayer.getDrawPile().size()
                        == cardCountBeforeEventReward + 5
                        && !testPlayer.hasRelic("relic_five_cards"),
                "五张牌事件奖励可展开为真实卡牌");
        EventRewardResolver.EventReward nukeReward =
                EventRewardResolver.applyRewards(new EventHandler.EventResult(
                                "获得 1 个核弹", 0, 0, "relic_nuke"),
                        testPlayer, new Random(1));
        ok &= assertCheck("relic_nuke".equals(nukeReward.getUnresolvedSpecialRewardId())
                        && !testPlayer.hasRelic("relic_nuke"),
                "核弹特殊奖励保持显式未接入状态");

        int oldMaxHp = testPlayer.getMaxHp();
        Relic hotWater = RelicFactory.createById("relic_hot_water_flask", testPlayer);
        new RelicManager(testPlayer).addRelic(hotWater);
        ok &= assertCheck(testPlayer.getMaxHp() == oldMaxHp + 5, "藏品即时效果生效: 热水壶");

        testPlayer.takeDamage(20);
        Potion healPotion = findPotion(potions, "potion_heal");
        int hpBeforeHeal = testPlayer.getHp();
        ok &= assertCheck(healPotion != null && healPotion.use(testPlayer, List.of(), new RelicManager(testPlayer))
                        && testPlayer.getHp() > hpBeforeHeal,
                "药水治疗效果生效: 回血药水");

        Enemy potionDummy = EntityFactory.createSimpleEnemy("potion_dummy", "药水假人", 30);
        Potion attackPotion = findPotion(potions, "potion_attack");
        ok &= assertCheck(attackPotion != null
                        && attackPotion.use(testPlayer, List.of(potionDummy), new RelicManager(testPlayer))
                        && potionDummy.getHp() < potionDummy.getMaxHp(),
                "药水伤害效果生效: 攻击药水");

        ShopManager shop = new ShopManager(testPlayer, new RelicManager(testPlayer));
        shop.generateItems();
        boolean hasRelic = shop.getItems().stream()
                .anyMatch(item -> item.getType() == ShopManager.ShopItem.ItemType.RELIC);
        boolean hasPotion = shop.getItems().stream()
                .anyMatch(item -> item.getType() == ShopManager.ShopItem.ItemType.POTION);
        ok &= assertCheck(hasRelic && hasPotion, "商店生成真实藏品和药水商品");

        List<Enemy> jsonEnemies = createEnemies(NodeType.FIGHT);
        ok &= assertCheck(!jsonEnemies.isEmpty()
                        && !"debug_dummy".equals(jsonEnemies.get(0).getId()),
                "命令行战斗从 JSON 怪物池创建敌人");

        println(ok ? "SELFTEST PASS" : "SELFTEST FAIL");
    }

    private boolean fixedEventResultsAreValid(List<DataLoader.EventData> events) {
        boolean ok = true;
        int count = 0;
        for (DataLoader.EventData event : events) {
            for (DataLoader.EventOptionData option : event.getOptions()) {
                if (!option.hasExecutableResult()) {
                    continue;
                }
                count++;
                EventHandler.EventResult result = EventResultResolver.resolve(option);
                if (result == null || result.description == null
                        || result.description.isBlank()) {
                    println("[SELFTEST] 固定事件结果无法解析: " + event.getName()
                            + " -> " + option.getText());
                    ok = false;
                }
                if (result != null && result.fullHeal && result.hpChange != 0) {
                    println("[SELFTEST] 固定事件结果同时声明 fullHeal 和 hpChange: "
                            + event.getName() + " -> " + option.getText());
                    ok = false;
                }
                if (option.hasRandomGoldChange()
                        && option.getGoldChangeMin() > option.getGoldChangeMax()) {
                    println("[SELFTEST] 事件随机金币范围反向: " + event.getName()
                            + " -> " + option.getText());
                    ok = false;
                }
                if (option.hasRandomHpChange()
                        && option.getHpChangeMin() > option.getHpChangeMax()) {
                    println("[SELFTEST] 事件随机生命范围反向: " + event.getName()
                            + " -> " + option.getText());
                    ok = false;
                }
                ok &= randomOutcomesAreValid(event, option);
                if (result != null && result.relicId != null
                        && !result.relicId.isBlank()
                        && !rewardIdIsKnown(result.relicId)) {
                    println("[SELFTEST] 固定事件结果引用未知奖励: " + event.getName()
                            + " -> " + option.getText() + " -> " + result.relicId);
                    ok = false;
                }
            }
        }
        if (count == 0) {
            println("[SELFTEST] 未找到可执行 JSON 固定事件结果");
            return false;
        }
        return ok;
    }

    private boolean randomOutcomesAreValid(DataLoader.EventData event,
                                           DataLoader.EventOptionData option) {
        if (!option.hasRandomOutcomes()) {
            return true;
        }
        boolean ok = true;
        int totalWeight = 0;
        for (DataLoader.EventOutcomeData outcome : option.getRandomOutcomes()) {
            if (outcome.getWeight() <= 0) {
                println("[SELFTEST] 事件随机 outcome 权重无效: " + event.getName()
                        + " -> " + option.getText());
                ok = false;
            }
            totalWeight += Math.max(0, outcome.getWeight());
            if (outcome.getOutcomeDescription() == null
                    || outcome.getOutcomeDescription().isBlank()) {
                println("[SELFTEST] 事件随机 outcome 缺少描述: " + event.getName()
                        + " -> " + option.getText());
                ok = false;
            }
            if (outcome.isFullHeal() && outcome.getHpChange() != 0) {
                println("[SELFTEST] 事件随机 outcome 同时声明 fullHeal 和 hpChange: "
                        + event.getName() + " -> " + option.getText());
                ok = false;
            }
            if (outcome.getRelicId() != null && !outcome.getRelicId().isBlank()
                    && !rewardIdIsKnown(outcome.getRelicId())) {
                println("[SELFTEST] 事件随机 outcome 引用未知奖励: " + event.getName()
                        + " -> " + option.getText() + " -> " + outcome.getRelicId());
                ok = false;
            }
        }
        if (totalWeight <= 0) {
            println("[SELFTEST] 事件随机 outcome 总权重无效: " + event.getName()
                    + " -> " + option.getText());
            ok = false;
        }
        return ok;
    }

    private boolean rewardIdIsKnown(String rewardId) {
        return EventRewardResolver.isSpecialRewardId(rewardId)
                || RelicFactory.createById(rewardId, player) != null;
    }

    private boolean eventOptionsDeclareExecution(List<DataLoader.EventData> events) {
        boolean ok = true;
        for (DataLoader.EventData event : events) {
            for (DataLoader.EventOptionData option : event.getOptions()) {
                if (option.hasExecutableResult()) {
                    continue;
                }
                if (!option.usesJavaExecutor()) {
                    println("[SELFTEST] 事件选项缺少固定结果或 Java executor 标记: "
                            + event.getName() + " -> " + option.getText());
                    ok = false;
                }
            }
        }
        return ok;
    }

    private boolean availableCardEffectsAreKnown(DataLoader loader) {
        boolean ok = true;
        for (Profession profession : Profession.values()) {
            String professionId = profession.name().toLowerCase();
            List<Card> cards = loader.loadCards(professionId);
            if (cards.isEmpty()) {
                continue;
            }
            ok &= cardEffectsAreKnown(profession.getDisplayName(), cards);
        }
        return ok;
    }

    private boolean cardEffectsAreKnown(String professionName, List<Card> cards) {
        boolean ok = true;
        for (Card card : cards) {
            for (CardEffect effect : CardEffectParser.parse(card.getEffects())) {
                if (!CardEffectParser.isKnownType(effect.getType())) {
                    println("[SELFTEST] 未知卡牌 effect: " + professionName + "/"
                            + card.getId()
                            + " -> " + effect.getRaw());
                    ok = false;
                } else if (!CardEffectParser.isExecutionSupported(effect.getType())) {
                    println("[SELFTEST] 卡牌 effect 尚未接入实战执行: " + professionName
                            + "/" + card.getId()
                            + " -> " + effect.getRaw());
                    ok = false;
                } else if (!CardEffectParser.hasValidArity(effect)) {
                    println("[SELFTEST] 卡牌 effect 参数数量不匹配: " + professionName
                            + "/" + card.getId()
                            + " -> " + effect.getRaw()
                            + "，期望分段数 " + CardEffectParser.expectedArity(effect.getType()));
                    ok = false;
                } else if (!CardEffectParser.hasValidArgumentTypes(effect)) {
                    println("[SELFTEST] 卡牌 effect 参数类型不匹配: " + professionName
                            + "/" + card.getId()
                            + " -> " + effect.getRaw()
                            + "，整数分段索引 " + CardEffectParser.expectedNumericParts(effect.getType()));
                    ok = false;
                } else if (!CardEffectParser.hasValidLiteralValues(effect)) {
                    println("[SELFTEST] 卡牌 effect 文字参数不支持: " + professionName
                            + "/" + card.getId()
                            + " -> " + effect.getRaw()
                            + "，期望 " + CardEffectParser.expectedLiteralValues(effect.getType()));
                    ok = false;
                }
            }
        }
        return ok;
    }

    private void runSeedTest(long seed) {
        println("开始种子自检: " + seed);
        boolean ok = true;
        String mapA = mapSignature(seed, 0);
        String mapB = mapSignature(seed, 0);
        String mapC = mapSignature(seed + 1, 0);
        ok &= assertCheck(mapA.equals(mapB), "同种子地图节点布局一致");
        ok &= assertCheck(!mapA.equals(mapC), "不同种子地图节点布局通常不同");

        String handA = openingHandSignature(seed, NodeType.FIGHT);
        String handB = openingHandSignature(seed, NodeType.FIGHT);
        String handC = openingHandSignature(seed + 1, NodeType.FIGHT);
        ok &= assertCheck(handA.equals(handB), "同种子战斗起手抽牌顺序一致: " + handA);
        ok &= assertCheck(!handA.equals(handC), "不同种子战斗起手抽牌通常不同");
        println(ok ? "SEEDTEST PASS" : "SEEDTEST FAIL");
    }

    private void runSaveTest() {
        println("开始存档自检...");
        boolean ok = true;
        long seed = 424242L;
        startNewRun(seed);
        int baselineHp = player.getHp();
        int baselineGold = player.getGold();
        LayerMapNode combatNode = firstAvailableCombatNode();
        if (combatNode == null) {
            combatNode = map.getAvailableNodes().isEmpty() ? map.getCurrentNode()
                    : map.getAvailableNodes().get(0);
        }
        runState.beginCombat(toNodeRef(combatNode));
        player.takeDamage(17);
        player.gainGold(33);
        ok &= assertCheck(saveManager.saveRun(runState), "战斗中可以保存对局");

        GameRunState loaded = saveManager.loadRun();
        ok &= assertCheck(loaded != null, "可以读取对局存档");
        if (loaded != null) {
            Player loadedPlayer = loaded.getPlayer();
            ok &= assertCheck(loadedPlayer.getHp() == baselineHp,
                    "战斗中 HP 变化未写入存档: " + loadedPlayer.getHp());
            ok &= assertCheck(loadedPlayer.getGold() == baselineGold,
                    "战斗中金币变化未写入存档: " + loadedPlayer.getGold());
            ok &= assertCheck(loaded.getCompletedNode() == null,
                    "战斗中退出不会记录该战斗节点已完成");
            ok &= assertCheck(!loaded.isInCombat(),
                    "读档后不保留战斗过程状态");
            ok &= assertCheck(openingHandSignature(seed, NodeType.FIGHT)
                            .equals(openingHandSignature(seed, NodeType.FIGHT)),
                    "读档后重新进入同节点可复现抽牌顺序");
        }

        startNewRun(seed + 1);
        player.setGold(90);
        GameRunState.NodeRef shopNode = new GameRunState.NodeRef(1, 2, 0, 6);
        runState.beginNode(shopNode);
        new NodeEntryResolver().enterNode(runState, shopNode);
        ok &= assertCheck(player.getGold() < 90,
                "非战斗节点入口效果已在保存前生效");
        ok &= assertCheck(saveManager.saveRun(runState), "非战斗节点中可以保存对局");

        GameRunState loadedNonCombat = saveManager.loadRun();
        ok &= assertCheck(loadedNonCombat != null, "可以读取非战斗节点中途存档");
        if (loadedNonCombat != null) {
            ok &= assertCheck(loadedNonCombat.getPlayer().getGold() == 90,
                    "非战斗节点中途存档应回到节点入口前金币: "
                            + loadedNonCombat.getPlayer().getGold());
            ok &= assertCheck(loadedNonCombat.getCompletedNode() == null,
                    "非战斗节点中途退出不会记录该节点已完成");
            ok &= assertCheck(loadedNonCombat.getActiveNode() == null,
                    "读档后不保留未完成节点过程状态");
        }

        startNewRun(seed + 2);
        player.setGold(90);
        GameRunState.NodeRef committedShopNode = new GameRunState.NodeRef(1, 2, 0, 6);
        runState.beginNode(committedShopNode);
        player.spendGold(25);
        runState.markActiveNodeProgressCommitted();
        ok &= assertCheck(saveManager.saveRun(runState),
                "已提交非战斗节点进度可以保存对局");

        GameRunState loadedCommittedNonCombat = saveManager.loadRun();
        ok &= assertCheck(loadedCommittedNonCombat != null,
                "可以读取已提交非战斗节点存档");
        if (loadedCommittedNonCombat != null) {
            ok &= assertCheck(loadedCommittedNonCombat.getPlayer().getGold() == 65,
                    "已提交非战斗节点存档保留玩家变化: "
                            + loadedCommittedNonCombat.getPlayer().getGold());
            ok &= assertCheck(loadedCommittedNonCombat.getCompletedNode() != null,
                    "已提交非战斗节点存档记录节点完成");
            ok &= assertCheck(loadedCommittedNonCombat.getActiveNode() == null,
                    "读档后不恢复已提交节点过程状态");
        }

        startNewRun(seed + 3);
        List<Potion> potions = new DataLoader().loadPotions();
        if (!potions.isEmpty()) {
            player.addPotion(potions.get(0).copy());
        }
        GameRunState.NodeRef potionEventNode = new GameRunState.NodeRef(1, 2, 0, 5);
        runState.beginNode(potionEventNode);
        if (!player.getPotions().isEmpty()) {
            player.removePotion(0);
        }
        runState.markActiveNodeProgressCommitted();
        ok &= assertCheck(saveManager.saveRun(runState),
                "已提交非战斗药水变化可以保存对局");

        GameRunState loadedPotionChange = saveManager.loadRun();
        ok &= assertCheck(loadedPotionChange != null,
                "可以读取已提交药水变化存档");
        if (loadedPotionChange != null) {
            ok &= assertCheck(loadedPotionChange.getPlayer().getPotions().isEmpty(),
                    "已提交非战斗药水变化会写入存档");
            ok &= assertCheck(loadedPotionChange.getCompletedNode() != null,
                    "已提交药水变化会记录节点完成");
        }
        println(ok ? "SAVETEST PASS" : "SAVETEST FAIL");
    }

    private String mapSignature(long seed, int layer) {
        LayerMapGraph testMap = new LayerMapGraph(configs.get(layer), seed, layer);
        StringBuilder builder = new StringBuilder();
        for (LayerMapNode[] column : testMap.getColumns()) {
            for (LayerMapNode node : column) {
                builder.append(node.getType().name().charAt(0));
            }
            builder.append('/');
        }
        return builder.toString();
    }

    private String openingHandSignature(long seed, NodeType nodeType) {
        Player testPlayer = new Player("seed-player", "种子战士", Profession.WARRIOR);
        GameRunState testRun = new GameRunState(seed, testPlayer);
        CombatEngine engine = new CombatEngine(testRun.getSeed(), "backend-combat:0:"
                + nodeType.name());
        engine.setBattleNodeType(nodeType);
        engine.setRelicManager(new RelicManager(testPlayer));
        engine.initBattle(testPlayer, List.of(EntityFactory.createSimpleEnemy(
                "seed_dummy", "种子假人", 1)));
        StringBuilder builder = new StringBuilder();
        for (Card card : testPlayer.getHand()) {
            if (!builder.isEmpty()) builder.append(',');
            builder.append(card.getId());
        }
        return builder.toString();
    }

    private LayerMapNode firstAvailableCombatNode() {
        for (LayerMapNode node : map.getAvailableNodes()) {
            if (node.getType().isCombat()) {
                return node;
            }
        }
        return null;
    }

    private Potion findPotion(List<Potion> potions, String id) {
        for (Potion potion : potions) {
            if (potion.getId().equals(id)) {
                return potion;
            }
        }
        return null;
    }

    private boolean assertCheck(boolean condition, String message) {
        println((condition ? "[OK] " : "[FAIL] ") + message);
        return condition;
    }

    private String shortName(NodeType type) {
        switch (type) {
            case FIGHT: return "战斗";
            case EMERGENCY: return "精英";
            case BOSS: return "Boss";
            case UNEXPECTED: return "事件";
            case REWARD: return "奖励";
            case SHOP: return "商店";
            case DECISION: return "抉择";
            case SAFEHOUSE: return "安全";
            default: return "未知";
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseSeedArg(String[] args, long fallback) {
        if (args == null) return fallback;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) continue;
            if (arg.startsWith("--seed=")) {
                return parseLong(arg.substring("--seed=".length()), fallback);
            }
            if ("--seed".equals(arg) && i + 1 < args.length) {
                return parseLong(args[i + 1], fallback);
            }
        }
        return fallback;
    }

    private List<Integer> parseCardIndexes(String value) {
        List<Integer> indexes = new ArrayList<>();
        for (String token : value.split(",")) {
            int index = parseInt(token.trim(), -1);
            if (index < 1) {
                return new ArrayList<>();
            }
            indexes.add(index - 1);
        }
        return indexes;
    }

    private String cardLabel(Card card, List<Card> cards) {
        int sameNameCount = 0;
        int occurrence = 0;
        for (Card current : cards) {
            if (current.getName().equals(card.getName())) {
                sameNameCount++;
                if (current == card) {
                    occurrence = sameNameCount;
                }
            }
        }
        if (sameNameCount <= 1) {
            return card.getName();
        }
        return card.getName() + "#" + occurrence;
    }

    private boolean handContainsInstance(List<Card> hand, Card card) {
        for (Card current : hand) {
            if (current == card) {
                return true;
            }
        }
        return false;
    }

    private String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private void print(String text) {
        System.out.print(text);
    }

    private void println(String text) {
        System.out.println(text);
    }

    private static class ConsoleNotifier implements ViewNotifier {
        @Override
        public void onBattleStart(AbstractEntity player, List<AbstractEntity> enemies) {
            System.out.println("[战斗] 开始，敌人数: " + enemies.size());
        }

        @Override
        public void onActionExecuted(CombatAction action) {
            System.out.println("[动作] " + action.getDescription());
        }

        @Override
        public void onBattleEnd(boolean victory, String reward) {
            System.out.println(victory ? "[战斗] 胜利: " + reward : "[战斗] 失败");
        }

        @Override
        public void onTurnStart(int turnNumber) {
            System.out.println("[回合] 第 " + turnNumber + " 回合");
        }

        @Override
        public void onCardPlayed(CombatAction action) {
            System.out.println("[出牌] " + action.getDescription());
        }
    }
}
