package com.fabricatedbook.desktop;

import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.engine.CardEffect;
import com.fabricatedbook.core.engine.CardEffectParser;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.EnemyActionResolver;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.encounter.EnemyEncounterResolver;
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
            case "flowtest":
                runNodeFlowTest();
                break;
            case "routetest":
                runRouteTest();
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
        println("  flowtest           验证非战斗节点提交后的自动保存语义");
        println("  routetest          验证隐藏路线、隐藏 Boss 和结局条件");
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

    private String nodeKey(LayerMapNode node) {
        if (node == null) return levelIndex + ":none";
        return levelIndex + ":" + node.getCol() + ":" + node.getRow()
                + ":" + node.getTypeCode();
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
            resolveShop(node);
        } else if (type == NodeType.REWARD) {
            resolveReward();
        } else if (type == NodeType.SAFEHOUSE) {
            int healed = player.heal(12);
            println("安全屋: 回复 " + healed + " 生命值。");
        } else if (type == NodeType.DECISION) {
            resolveDecision();
        } else {
            resolveEvent();
        }
    }

    private void resolveShop(LayerMapNode node) {
        ShopManager shop = new ShopManager(player, new RelicManager(player), runState,
                "shop:" + nodeKey(node));
        shop.generateItems();
        println("进入商店。金币: " + player.getGold()
                + "，删牌价格: " + shop.getRemoveCost());
        printShopHelp();
        printShop(shop);
        while (running) {
            print("shop> ");
            String line = readLine();
            if (line == null) {
                running = false;
                println("");
                println("输入结束，已退出。");
                break;
            }
            line = line.trim();
            if (line.isEmpty() || "leave".equalsIgnoreCase(line)
                    || "exit".equalsIgnoreCase(line)) {
                break;
            }
            handleShopCommand(shop, line);
        }
    }

    private void handleShopCommand(ShopManager shop, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        switch (command) {
            case "help" -> printShopHelp();
            case "list" -> printShop(shop);
            case "deck" -> printDeck();
            case "potions" -> printPotions();
            case "buy" -> buyShopItem(shop, parts);
            case "remove" -> removeShopCard(shop, parts);
            case "save" -> saveRun();
            default -> {
                println("未知商店命令: " + command);
                printShopHelp();
            }
        }
    }

    private void printShopHelp() {
        println("商店命令:");
        println("  list              查看商品");
        println("  buy <编号>         购买商品");
        println("  remove <牌序号>    支付删牌服务移除抽牌堆中的牌");
        println("  deck              查看当前牌堆");
        println("  potions           查看药水栏");
        println("  save              保存当前对局");
        println("  leave             离开商店");
    }

    private void printShop(ShopManager shop) {
        println("商品列表（金币 " + player.getGold() + "）:");
        List<ShopManager.ShopItem> items = shop.getItems();
        for (int i = 0; i < items.size(); i++) {
            ShopManager.ShopItem item = items.get(i);
            println("  " + (i + 1) + ". [" + item.getType() + "] "
                    + item.getName() + " - " + item.getPrice() + " 金币"
                    + (item.isPurchased() ? " (已购买)" : ""));
        }
        println("  remove. 删牌服务 - " + shop.getRemoveCost() + " 金币"
                + (shop.isRemovePurchased() ? " (已购买)" : ""));
    }

    private void buyShopItem(ShopManager shop, String[] parts) {
        if (parts.length < 2) {
            println("用法: buy <商品编号>");
            return;
        }
        int index = parseInt(parts[1], -1) - 1;
        if (shop.purchase(index)) {
            ShopManager.ShopItem item = shop.getItems().get(index);
            println("购买成功: " + item.getName());
            markShopProgressCommittedAndSave();
            printShop(shop);
        } else {
            println("购买失败，请检查编号、金币、药水栏或购买状态。");
        }
    }

    private void removeShopCard(ShopManager shop, String[] parts) {
        if (parts.length < 2) {
            println("用法: remove <抽牌堆牌序号>");
            printDrawPile();
            return;
        }
        int index = parseInt(parts[1], -1) - 1;
        if (shop.purchaseRemove(index)) {
            println("删牌成功。");
            markShopProgressCommittedAndSave();
            printDrawPile();
            printShop(shop);
        } else {
            println("删牌失败，请检查牌序号、金币或是否已购买删牌服务。");
            printDrawPile();
        }
    }

    private void markShopProgressCommittedAndSave() {
        runState.markActiveNodeProgressCommitted();
        if (saveManager.saveRun(runState)) {
            println("商店进度已自动保存。");
        } else {
            println("商店进度自动保存失败。");
        }
    }

    private void resolveDecision() {
        Random eventRandom = randomForNode("decision-result", map.getCurrentNode());
        String eventName = levelIndex >= 3 ? "命运抉择2" : "命运抉择1";
        resolveNamedEvent("命运抉择", eventName, eventRandom);
    }

    private void resolveReward() {
        resolveNamedEvent("奖励事件", "好诗歪诗",
                randomForNode("reward-result", map.getCurrentNode()));
    }

    private void resolveNamedEvent(String label, String eventName, Random eventRandom) {
        EventHandler handler = new EventHandler(eventRandom);
        List<EventHandler.EventOption> options = handler.getOptions(eventName, player);
        println(label + ": " + eventName);
        println(handler.getEventDescription(eventName));
        for (int i = 0; i < options.size(); i++) {
            EventHandler.EventOption option = options.get(i);
            println("  " + (i + 1) + ". " + option.label + " - " + option.description);
        }
        println("输入选项编号，或回车选择 1。");
        print("event> ");
        String line = readLine();
        if (line == null) {
            line = "";
        }
        int choice = line.trim().isEmpty() ? 1 : parseInt(line.trim(), 1);
        choice = Math.max(1, Math.min(choice, options.size()));

        EventHandler.EventResult result = handler.executeEvent(eventName,
                choice - 1, player);
        applyEventResult(result, eventRandom);
        println(result.description);
        handleEventOutcome(result);
    }

    private void resolveEvent() {
        Random eventRandom = randomForNode("event-result", map.getCurrentNode());
        List<String> names = new EventHandler(eventRandom).getEventNames();
        String eventName = names.get(randomForNode("event-name", map.getCurrentNode())
                .nextInt(names.size()));
        resolveNamedEvent("事件", eventName, eventRandom);
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

    private void handleEventOutcome(EventHandler.EventResult result) {
        if (result.outcome == null || result.outcome.isBlank()) {
            return;
        }
        switch (result.outcome) {
            case "ENDING_INTERRUPTED" -> {
                println("隐藏结局: 讲述中断。");
                running = false;
            }
            case "ENDING_HIDDEN" -> {
                println("隐藏结局触发。");
                running = false;
            }
            default -> println("事件 outcome: " + result.outcome);
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
        EnemyEncounterResolver.EncounterResult result = EnemyEncounterResolver.resolve(
                player, groups, level, nodeType,
                runState.randomFor("backend-enemies", levelIndex, nodeType.name()));
        if (!result.isFallback()) {
            DataLoader.EnemyGroup group = result.getGroup();
            if (group != null) {
                println("敌人组: " + group.getName() + " (" + group.getId() + ")");
            }
            return result.getEnemies();
        }

        println("未找到 JSON 敌人组，使用 fallback 调试敌人。");
        return result.getEnemies();
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

    private void printDrawPile() {
        println("抽牌堆:");
        if (player.getDrawPile().isEmpty()) {
            println("  (空)");
            return;
        }
        for (int i = 0; i < player.getDrawPile().size(); i++) {
            Card card = player.getDrawPile().get(i);
            println("  " + (i + 1) + ". [" + card.getCost() + "] "
                    + cardLabel(card, player.getDrawPile()));
        }
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
        ok &= assertCheck(cardPoolMatchesJson(cards), "运行时战士 CardPool 与 JSON 一致");
        ok &= assertCheck(availableCardEffectsAreKnown(loader),
                "已配置职业 JSON 卡牌 effect 均已接入实战 DSL");
        ok &= assertCheck(cardEffectHandlersMatchRegistry(),
                "卡牌 effect 执行/预览 handler 与注册表一致");
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
        ok &= assertCheck(allEnemyActionsAreResolvable(loader),
                "已配置怪物 actionScript 均已接入 EnemyActionResolver");

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
        Player decisionPlayer = new Player("selftest-decision-relic",
                "自检战士", Profession.WARRIOR);
        Relic betrayal = RelicFactory.createById("relic_betrayal", decisionPlayer);
        if (betrayal != null) {
            decisionPlayer.addRelic(betrayal);
        }
        ok &= assertCheck(eventHandler.getOptions("命运抉择2", decisionPlayer)
                        .size() == 3,
                "命运抉择条件选项按玩家藏品展示");
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
        ok &= assertCheck(nukeReward.getPotion() != null
                        && Potion.NUKE_ID.equals(nukeReward.getPotion().getId())
                        && testPlayer.getPotions().stream()
                                .anyMatch(potion -> Potion.NUKE_ID.equals(potion.getId()))
                        && !testPlayer.hasRelic("relic_nuke"),
                "核弹事件奖励发放特殊药水且不加入藏品栏");

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
        ok &= assertCheck(centralizationGrowthWorks(),
                "集权进入战斗节点后持续成长并修正伤害");
        ok &= assertCheck(routeRelicsModifyFifthFloorEnemyHp(),
                "背叛/仇恨在第 5 层修正敌人生命值");

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

    private boolean centralizationGrowthWorks() {
        Player centralizationPlayer = new Player("selftest-centralization",
                "集权自检战士", Profession.WARRIOR);
        Relic centralization = RelicFactory.createById("relic_centralization",
                centralizationPlayer);
        if (centralization == null) {
            return false;
        }
        centralizationPlayer.addRelic(centralization);
        GameRunState state = new GameRunState(616161L, centralizationPlayer);
        GameRunState.NodeRef fight = new GameRunState.NodeRef(0, 0, 0, 1);
        NodeEntryResolver resolver = new NodeEntryResolver();
        resolver.enterNode(state, fight);
        resolver.enterNode(state, new GameRunState.NodeRef(0, 1, 0, 2));

        RelicManager relicManager = new RelicManager(centralizationPlayer);
        Enemy dummy = EntityFactory.createSimpleEnemy("centralization_dummy",
                "集权假人", 50);
        return centralizationPlayer.getCentralizationCombatEntries() == 2
                && relicManager.modifyDamage(100, centralizationPlayer, dummy) == 110;
    }

    private boolean routeRelicsModifyFifthFloorEnemyHp() {
        Player betrayalPlayer = new Player("selftest-betrayal",
                "背叛自检战士", Profession.WARRIOR);
        betrayalPlayer.setCurrentFloor(5);
        Relic betrayal = RelicFactory.createById("relic_betrayal", betrayalPlayer);
        if (betrayal == null) {
            return false;
        }
        betrayalPlayer.addRelic(betrayal);
        Enemy strongerEnemy = EntityFactory.createSimpleEnemy(
                "betrayal_dummy", "背叛假人", 100);
        new RelicManager(betrayalPlayer).modifyEnemiesAtCombatStart(
                List.of(strongerEnemy));

        Player hatredPlayer = new Player("selftest-hatred",
                "仇恨自检战士", Profession.WARRIOR);
        hatredPlayer.setCurrentFloor(5);
        Relic hatred = RelicFactory.createById("relic_hatred", hatredPlayer);
        if (hatred == null) {
            return false;
        }
        hatredPlayer.addRelic(hatred);
        Enemy weakerEnemy = EntityFactory.createSimpleEnemy(
                "hatred_dummy", "仇恨假人", 100);
        new RelicManager(hatredPlayer).modifyEnemiesAtCombatStart(
                List.of(weakerEnemy));

        return strongerEnemy.getMaxHp() == 120 && strongerEnemy.getHp() == 120
                && weakerEnemy.getMaxHp() == 80 && weakerEnemy.getHp() == 80;
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

    private boolean allEnemyActionsAreResolvable(DataLoader loader) {
        boolean ok = true;
        Player testPlayer = new Player("selftest-enemy-actions",
                "敌人行动自检", Profession.WARRIOR);
        for (int level = 1; level <= 5; level++) {
            for (DataLoader.EnemyGroup group : loader.loadMonsters(level)) {
                List<Enemy> enemies = new ArrayList<>();
                for (DataLoader.EnemyData enemyData : group.getEnemies()) {
                    enemies.add(enemyData.toEnemy());
                }
                for (Enemy enemy : enemies) {
                    if (enemy.getActionScript() == null) {
                        println("[SELFTEST] 怪物缺少 actionScript: level" + level
                                + "/" + group.getId() + "/" + enemy.getId());
                        ok = false;
                        continue;
                    }
                    for (String actionId : enemy.getActionScript()) {
                        List<CombatAction> actions = EnemyActionResolver.resolve(enemy,
                                actionId, testPlayer, enemies, new Random(1));
                        if (actions == null) {
                            println("[SELFTEST] 怪物 actionScript 未接入 resolver: level"
                                    + level + "/" + group.getId() + "/"
                                    + enemy.getId() + " -> " + actionId);
                            ok = false;
                        }
                    }
                }
            }
        }
        return ok;
    }

    private boolean cardPoolMatchesJson(List<Card> jsonCards) {
        List<Card> runtimeCards = CardPool.getCardsByProfession("warrior");
        boolean ok = true;
        if (runtimeCards.size() != jsonCards.size()) {
            println("[SELFTEST] 战士 CardPool 数量与 JSON 不一致: runtime="
                    + runtimeCards.size() + ", json=" + jsonCards.size());
            ok = false;
        }
        for (Card jsonCard : jsonCards) {
            Card runtimeCard = CardPool.findById(jsonCard.getId());
            if (runtimeCard == null) {
                println("[SELFTEST] 战士 CardPool 缺少 JSON 卡牌: " + jsonCard.getId());
                ok = false;
                continue;
            }
            if (!runtimeCard.getName().equals(jsonCard.getName())
                    || runtimeCard.getCost() != jsonCard.getCost()
                    || runtimeCard.getType() != jsonCard.getType()
                    || runtimeCard.getRarity() != jsonCard.getRarity()
                    || runtimeCard.getValue() != jsonCard.getValue()
                    || !runtimeCard.getEffects().equals(jsonCard.getEffects())) {
                println("[SELFTEST] 战士 CardPool 卡牌字段与 JSON 不一致: "
                        + jsonCard.getId());
                ok = false;
            }
        }
        return ok;
    }

    private boolean cardEffectHandlersMatchRegistry() {
        boolean ok = true;
        if (!CardEffectParser.missingExecutionHandlers().isEmpty()) {
            println("[SELFTEST] 缺少卡牌 effect 执行 handler: "
                    + CardEffectParser.missingExecutionHandlers());
            ok = false;
        }
        if (!CardEffectParser.missingPreviewHandlers().isEmpty()) {
            println("[SELFTEST] 缺少卡牌 effect 预览 handler: "
                    + CardEffectParser.missingPreviewHandlers());
            ok = false;
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

    private void runNodeFlowTest() {
        println("开始节点流程自检...");
        SaveManager flowSave = new SaveManager("build/backend-flowtest-save.json");
        boolean ok = true;
        ok &= eventChoiceFlowPersists(flowSave);
        ok &= rewardEventFlowPersists(flowSave);
        ok &= shopPurchaseFlowPersists(flowSave);
        ok &= shopRemoveFlowPersists(flowSave);
        ok &= safeHouseFlowPersists(flowSave);
        ok &= nonCombatPotionDiscardFlowPersists(flowSave);
        println(ok ? "FLOWTEST PASS" : "FLOWTEST FAIL");
    }

    private void runRouteTest() {
        println("开始隐藏路线自检...");
        boolean ok = true;
        DataLoader loader = new DataLoader();

        Player ordinaryPlayer = new Player("route-normal", "普通路线战士", Profession.WARRIOR);
        EnemyEncounterResolver.EncounterResult normalBoss =
                EnemyEncounterResolver.resolve(ordinaryPlayer, loader.loadMonsters(5),
                        5, NodeType.BOSS, new Random(1));
        ok &= assertCheck(groupName(normalBoss).equals("魔王"),
                "普通路线第 5 层 Boss 为魔王");

        Player hiddenPlayer = new Player("route-hidden", "隐藏路线战士", Profession.WARRIOR);
        addRelicById(hiddenPlayer, "relic_betrayal");
        addRelicById(hiddenPlayer, "relic_babel_tower");
        EnemyEncounterResolver.EncounterResult hiddenBoss =
                EnemyEncounterResolver.resolve(hiddenPlayer, loader.loadMonsters(5),
                        5, NodeType.BOSS, new Random(1));
        ok &= assertCheck(groupName(hiddenBoss).equals("幕后黑手"),
                "背叛/仇恨 + 巴别塔路线第 5 层 Boss 为幕后黑手");
        ok &= assertCheck(hiddenBoss.getEnemies().stream()
                        .anyMatch(enemy -> "puppet_master".equals(enemy.getId())),
                "隐藏 Boss 敌人组包含幕后黑手实体");
        ok &= assertCheck(EnemyEncounterResolver.isHiddenBossRoute(hiddenPlayer),
                "隐藏路线条件由 core 统一判断");

        EventHandler handler = new EventHandler(new Random(1));
        Player decisionPlayer = new Player("route-decision", "抉择战士", Profession.WARRIOR);
        addRelicById(decisionPlayer, "relic_hatred");
        List<EventHandler.EventOption> options = handler.getOptions("命运抉择2", decisionPlayer);
        EventHandler.EventResult result = handler.executeEvent("命运抉择2", 2, decisionPlayer);
        EventRewardResolver.applyRewards(result, decisionPlayer, new Random(1));
        ok &= assertCheck(options.size() == 3 && "relic_babel_tower".equals(result.relicId)
                        && decisionPlayer.hasRelic("relic_babel_tower"),
                "门扉隐藏选项可授予巴别塔并开启隐藏 Boss 路线");

        EventHandler.EventResult interrupted = handler.executeEvent("命运抉择1", 1);
        ok &= assertCheck("ENDING_INTERRUPTED".equals(interrupted.outcome),
                "第一层回头仍触发讲述中断结局");

        println(ok ? "ROUTETEST PASS" : "ROUTETEST FAIL");
    }

    private String groupName(EnemyEncounterResolver.EncounterResult result) {
        return result != null && result.getGroup() != null
                ? result.getGroup().getName() : "";
    }

    private void addRelicById(Player target, String relicId) {
        Relic relic = RelicFactory.createById(relicId, target);
        if (relic != null) {
            target.addRelic(relic);
        }
    }

    private boolean eventChoiceFlowPersists(SaveManager flowSave) {
        startNewRun(515001L);
        GameRunState.NodeRef eventNode = nodeRef(NodeType.UNEXPECTED, 1, 0);
        runState.beginNode(eventNode);
        EventHandler.EventResult result = new EventHandler(new Random(1))
                .executeEvent("相遇", 0, player);
        applyEventResult(result, new Random(1));
        runState.markActiveNodeProgressCommitted();

        boolean ok = assertCheck(flowSave.saveRun(runState),
                "事件选择提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "事件选择提交后可以读档");
        if (loaded != null) {
            ok &= assertCheck(loaded.getPlayer().hasRelic("relic_betrayal"),
                    "事件选择奖励藏品写入存档");
            ok &= assertCheck(isCompletedNode(loaded, eventNode),
                    "事件选择提交后记录节点完成");
        }
        return ok;
    }

    private boolean rewardEventFlowPersists(SaveManager flowSave) {
        startNewRun(515006L);
        int beforeDeckSize = player.getDrawPile().size();
        GameRunState.NodeRef rewardNode = nodeRef(NodeType.REWARD, 1, 0);
        runState.beginNode(rewardNode);
        Random rewardRandom = new Random(2);
        EventHandler.EventResult result = new EventHandler(rewardRandom)
                .executeEvent("好诗歪诗", 0, player);
        applyEventResult(result, rewardRandom);
        runState.markActiveNodeProgressCommitted();

        boolean ok = assertCheck(flowSave.saveRun(runState),
                "奖励节点事件提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "奖励节点事件提交后可以读档");
        if (loaded != null) {
            ok &= assertCheck(loaded.getPlayer().getDrawPile().size()
                            >= beforeDeckSize + 5,
                    "奖励节点事件奖励写入存档");
            ok &= assertCheck(isCompletedNode(loaded, rewardNode),
                    "奖励节点事件提交后记录节点完成");
        }
        return ok;
    }

    private boolean shopPurchaseFlowPersists(SaveManager flowSave) {
        startNewRun(515002L);
        player.setGold(1000);
        int beforeDeckSize = player.getDrawPile().size();
        int beforeRelics = player.getRelics().size();
        int beforePotions = player.getPotions().size();
        GameRunState.NodeRef shopNode = nodeRef(NodeType.SHOP, 1, 0);
        runState.beginNode(shopNode);
        ShopManager shop = new ShopManager(player, new RelicManager(player), runState,
                "flowtest-shop-buy");
        shop.generateItems();
        ShopManager.ShopItem firstItem = shop.getItems().isEmpty() ? null
                : shop.getItems().get(0);
        boolean purchased = firstItem != null && shop.purchase(0);
        if (purchased) {
            runState.markActiveNodeProgressCommitted();
        }

        boolean ok = assertCheck(purchased, "商店商品可购买");
        ok &= assertCheck(flowSave.saveRun(runState), "商店商品购买提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "商店商品购买提交后可以读档");
        if (loaded != null && firstItem != null) {
            ok &= assertCheck(shopPurchasePersisted(loaded.getPlayer(), firstItem,
                            beforeDeckSize, beforeRelics, beforePotions),
                    "商店商品购买结果写入存档");
            ok &= assertCheck(isCompletedNode(loaded, shopNode),
                    "商店商品购买提交后记录节点完成");
        }
        return ok;
    }

    private boolean shopRemoveFlowPersists(SaveManager flowSave) {
        startNewRun(515005L);
        player.setGold(150);
        int beforeDeckSize = player.getDrawPile().size();
        GameRunState.NodeRef shopNode = nodeRef(NodeType.SHOP, 1, 0);
        runState.beginNode(shopNode);
        ShopManager shop = new ShopManager(player, new RelicManager(player), runState,
                "flowtest-shop");
        shop.generateItems();
        boolean removed = shop.purchaseRemove(0);
        if (removed) {
            runState.markActiveNodeProgressCommitted();
        }

        boolean ok = assertCheck(removed, "商店删牌服务可执行");
        ok &= assertCheck(flowSave.saveRun(runState), "商店删牌提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "商店删牌提交后可以读档");
        if (loaded != null) {
            ok &= assertCheck(loaded.getPlayer().getDrawPile().size() == beforeDeckSize - 1,
                    "商店删牌结果写入存档");
            ok &= assertCheck(loaded.getShopRemoveCount() == 1,
                    "商店删牌次数写入存档");
            ok &= assertCheck(isCompletedNode(loaded, shopNode),
                    "商店删牌提交后记录节点完成");
        }
        return ok;
    }

    private boolean shopPurchasePersisted(Player loadedPlayer,
                                          ShopManager.ShopItem purchasedItem,
                                          int beforeDeckSize,
                                          int beforeRelics,
                                          int beforePotions) {
        return switch (purchasedItem.getType()) {
            case CARD -> loadedPlayer.getDrawPile().size() == beforeDeckSize + 1;
            case RELIC -> loadedPlayer.getRelics().size() == beforeRelics + 1;
            case POTION -> loadedPlayer.getPotions().size() == beforePotions + 1;
        };
    }

    private boolean safeHouseFlowPersists(SaveManager flowSave) {
        startNewRun(515003L);
        player.takeDamage(30);
        int beforeHeal = player.getHp();
        GameRunState.NodeRef safeHouseNode = nodeRef(NodeType.SAFEHOUSE, 1, 0);
        runState.beginNode(safeHouseNode);
        player.heal(12);
        runState.markActiveNodeProgressCommitted();

        boolean ok = assertCheck(flowSave.saveRun(runState),
                "安全屋结算提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "安全屋结算提交后可以读档");
        if (loaded != null) {
            ok &= assertCheck(loaded.getPlayer().getHp() > beforeHeal,
                    "安全屋治疗结果写入存档");
            ok &= assertCheck(isCompletedNode(loaded, safeHouseNode),
                    "安全屋结算提交后记录节点完成");
        }
        return ok;
    }

    private boolean nonCombatPotionDiscardFlowPersists(SaveManager flowSave) {
        startNewRun(515004L);
        List<Potion> potions = new DataLoader().loadPotions();
        if (!potions.isEmpty()) {
            player.addPotion(potions.get(0).copy());
        }
        GameRunState.NodeRef eventNode = nodeRef(NodeType.UNEXPECTED, 1, 0);
        runState.beginNode(eventNode);
        if (!player.getPotions().isEmpty()) {
            player.removePotion(0);
        }
        runState.markActiveNodeProgressCommitted();

        boolean ok = assertCheck(flowSave.saveRun(runState),
                "非战斗药水丢弃提交后可以保存对局");
        GameRunState loaded = flowSave.loadRun();
        ok &= assertCheck(loaded != null, "非战斗药水丢弃提交后可以读档");
        if (loaded != null) {
            ok &= assertCheck(loaded.getPlayer().getPotions().isEmpty(),
                    "非战斗药水丢弃结果写入存档");
            ok &= assertCheck(isCompletedNode(loaded, eventNode),
                    "非战斗药水丢弃提交后记录节点完成");
        }
        return ok;
    }

    private GameRunState.NodeRef nodeRef(NodeType type, int col, int row) {
        return new GameRunState.NodeRef(levelIndex, col, row, nodeTypeCode(type));
    }

    private int nodeTypeCode(NodeType type) {
        return switch (type) {
            case FIGHT -> 1;
            case EMERGENCY -> 2;
            case BOSS -> 3;
            case UNEXPECTED -> 4;
            case REWARD -> 5;
            case SHOP -> 6;
            case DECISION -> 8;
            case SAFEHOUSE -> 9;
        };
    }

    private boolean isCompletedNode(GameRunState state, GameRunState.NodeRef expected) {
        GameRunState.NodeRef actual = state.getCompletedNode();
        return actual != null
                && actual.layer == expected.layer
                && actual.col == expected.col
                && actual.row == expected.row
                && actual.type == expected.type;
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
