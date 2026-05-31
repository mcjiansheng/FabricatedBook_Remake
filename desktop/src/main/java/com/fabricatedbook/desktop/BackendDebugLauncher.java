package com.fabricatedbook.desktop;

import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.event.EventHandler;
import com.fabricatedbook.core.map.MapConfig;
import com.fabricatedbook.core.map.MapGraph;
import com.fabricatedbook.core.map.Node;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.relic.RelicManager;

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
    private final Random random = new Random();
    private final List<MapConfig> configs = List.of(
            MapConfig.wilderness(),
            MapConfig.forest(),
            MapConfig.mysticForest(),
            MapConfig.mist(),
            MapConfig.tower()
    );

    private Player player;
    private MapGraph map;
    private int levelIndex;
    private boolean running = true;

    public static void main(String[] args) {
        new BackendDebugLauncher().run();
    }

    private void run() {
        CardPool.getCardsByProfession("warrior");
        player = new Player("debug-player", "调试战士", Profession.WARRIOR);
        player.setGold(80);
        levelIndex = 0;
        createMap();

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
            case "deck":
                printDeck();
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
        println("  map                打印当前层地图");
        println("  routes             查看当前位置可选路线");
        println("  choose <编号>       选择一条可达路线并进入节点");
        println("  battle             直接启动一场普通战斗");
        println("  deck               查看当前牌堆/手牌/弃牌堆数量");
        println("  newmap             重新生成当前层地图");
        println("  quit               退出调试控制台");
        println("");
        println("战斗命令会在进入战斗后显示。");
    }

    private void createMap() {
        MapConfig config = configs.get(levelIndex);
        map = new MapGraph(config);
        player.setCurrentFloor(config.getLevel());
    }

    private void printStatus() {
        println("玩家: " + player.getName()
                + " HP " + player.getHp() + "/" + player.getMaxHp()
                + " 金币 " + player.getGold()
                + " 当前层 " + configs.get(levelIndex).getLevelName());
    }

    private void printMap() {
        MapConfig config = map.getConfig();
        println("");
        println("地图: " + config.getLevelName()
                + " (" + config.getWidth() + "x" + config.getHeight() + ")"
                + " 环境: " + config.getEnvironmentEffect());
        for (List<Node> row : map.getGrid()) {
            StringBuilder builder = new StringBuilder();
            for (Node node : row) {
                String marker = " ";
                if (node == map.getPlayerPosition()) {
                    marker = "@";
                } else if (node.isVisited()) {
                    marker = "*";
                } else if (map.getAvailableNodes().contains(node)) {
                    marker = "!";
                }
                builder.append(String.format("[%s%d,%d %-4s] ",
                        marker,
                        node.getRow(),
                        node.getCol(),
                        shortName(node.getType())));
            }
            println(builder.toString());
        }
        printRoutes();
    }

    private void printRoutes() {
        List<Node> available = map.getAvailableNodes();
        if (available.isEmpty()) {
            println("当前没有可选路线。");
            return;
        }
        println("可选路线:");
        for (int i = 0; i < available.size(); i++) {
            Node node = available.get(i);
            println("  " + (i + 1) + ". " + node.getType().getDisplayName()
                    + " (" + node.getRow() + "," + node.getCol() + ")");
        }
    }

    private void chooseRoute(int index) {
        List<Node> available = map.getAvailableNodes();
        if (index < 1 || index > available.size()) {
            println("路线编号无效。");
            printRoutes();
            return;
        }

        Node node = available.get(index - 1);
        if (!map.moveTo(node)) {
            println("移动失败: " + node);
            return;
        }

        println("进入节点: " + node.getType().getDisplayName()
                + " (" + node.getRow() + "," + node.getCol() + ")");
        resolveNode(node);

        if (!running) {
            return;
        }
        if (map.isAtEnd()) {
            advanceLevel();
        }
        printStatus();
        printMap();
    }

    private void resolveNode(Node node) {
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
        } else {
            resolveEvent();
        }
    }

    private void resolveEvent() {
        EventHandler handler = new EventHandler();
        List<String> names = handler.getEventNames();
        String eventName = names.get(random.nextInt(names.size()));
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
        applyEventResult(result);
        println(result.description);
    }

    private void applyEventResult(EventHandler.EventResult result) {
        if (result.goldChange > 0) {
            player.gainGold(result.goldChange);
        } else if (result.goldChange < 0) {
            player.spendGold(-result.goldChange);
        }

        if (result.hpChange > 0) {
            if (result.hpChange >= 9999) {
                player.heal(player.getMaxHp());
            } else {
                player.heal(result.hpChange);
            }
        } else if (result.hpChange < 0) {
            player.takeDamage(-result.hpChange);
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
        CombatEngine engine = new CombatEngine();
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

    private void playCard(CombatEngine engine, String[] parts) {
        if (parts.length < 2) {
            println("用法: play <手牌编号> [敌人编号]");
            return;
        }
        int cardIndex = parseInt(parts[1], -1) - 1;
        List<Card> hand = engine.getPlayer().getHand();
        if (cardIndex < 0 || cardIndex >= hand.size()) {
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

        Card card = hand.get(cardIndex);
        boolean played = engine.playCard(card, target);
        if (!played) {
            println("出牌失败，可能是能量不足或卡牌已不在手牌。");
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
        List<Enemy> enemies = new ArrayList<>();
        switch (nodeType) {
            case BOSS:
                enemies.add(new Enemy("debug_boss", "命令行首领", 70,
                        List.of("atk8", "def8", "atk12")));
                break;
            case EMERGENCY:
                enemies.add(new Enemy("debug_elite", "命令行精英", 48,
                        List.of("atk7", "atk5x2", "def10")));
                break;
            default:
                enemies.add(EntityFactory.createSimpleEnemy("debug_dummy", "命令行假人", 36));
                break;
        }
        return enemies;
    }

    private void printBattleHelp() {
        println("");
        println("战斗命令:");
        println("  state              查看战斗状态");
        println("  hand               查看手牌");
        println("  enemies            查看敌人");
        println("  play <牌> [敌人]    使用手牌，编号从 1 开始");
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
                    + card.getName() + " - " + card.getDescription());
        }
    }

    private void printDeck() {
        println("牌堆: 抽牌堆 " + player.getDrawPile().size()
                + " / 手牌 " + player.getHand().size()
                + " / 弃牌堆 " + player.getDiscardPile().size());
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
