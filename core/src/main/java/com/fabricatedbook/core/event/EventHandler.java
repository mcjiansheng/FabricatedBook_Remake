package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.data.DataLoader;

import java.util.*;

/**
 * EventHandler — 游戏事件处理器
 * <p>
 * 实现所有随机事件（参考 game_encyclopedia.md "九、事件列表"）。
 * 每个事件包含选项列表和对应结果。
 * 事件执行可能修改玩家状态（生命值、金币、藏品等）或进入战斗。
 * <p>
 * 引用方：EventScreen（选择事件选项时调用）、GameController（触发事件）
 */
public class EventHandler {

    /** 随机数生成器 */
    private final Random random;
    private final Map<String, DataLoader.EventData> eventsByName;

    /** 事件结果回调接口 */
    @FunctionalInterface
    public interface EventCallback {
        /**
         * 事件执行结果通知。
         *
         * @param description 结果描述文本
         * @param goldChange  金币变化（正数为获得）
         * @param hpChange    生命值变化（正数为回复）
         * @param relicId     获得的藏品 ID（无则为 null）
         */
        void onResult(String description, int goldChange,
                      int hpChange, String relicId);
    }

    /**
     * 事件选项。
     */
    public static class EventOption {
        public final String label;
        public final String description;

        public EventOption(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    /**
     * 事件结果。
     */
    public static class EventResult {
        public final String description;
        public final int goldChange;
        public final int hpChange;
        public final String relicId;
        public final String outcome;

        public EventResult(String description, int goldChange,
                           int hpChange, String relicId) {
            this(description, goldChange, hpChange, relicId, null);
        }

        public EventResult(String description, int goldChange,
                           int hpChange, String relicId, String outcome) {
            this.description = description;
            this.goldChange = goldChange;
            this.hpChange = hpChange;
            this.relicId = relicId;
            this.outcome = outcome;
        }
    }

    public EventHandler() {
        this(new Random());
    }

    public EventHandler(Random random) {
        this.random = random == null ? new Random() : random;
        this.eventsByName = loadEventsByName();
    }

    /**
     * 获取事件名称列表。
     *
     * @return 所有可用事件的名称列表
     */
    public List<String> getEventNames() {
        if (!eventsByName.isEmpty()) {
            return new ArrayList<>(eventsByName.keySet());
        }
        return hardcodedEventNames();
    }

    public String getEventDescription(String eventName) {
        DataLoader.EventData eventData = eventsByName.get(eventName);
        if (eventData != null && eventData.getDescription() != null
                && !eventData.getDescription().isBlank()) {
            return eventData.getDescription();
        }
        return switch (eventName) {
            case "命运抉择1" -> "迷雾渐起，前途未知，你将何去何从？";
            case "命运抉择2" -> "你战胜了强大的敌人，但这只是见到“幕后黑手”的前提。\n他要求你前往高塔帮他“解决”一个麻烦，同时可以给予你一个东西。";
            case "相遇" -> "一位神秘旅人拦住了你的去路，提出同行。你能感觉到这份邀请背后藏着某种代价。";
            case "翅膀雕像" -> "一座破损的翅膀雕像伫立在路旁，石缝里透出微弱的光。";
            case "黏液世界" -> "脚下的地面变得湿滑，半透明的黏液从四周缓慢涌来。";
            case "投资" -> "陌生商人向你展示一份看似荒唐的投资计划，承诺会给出丰厚回报。";
            case "追猎" -> "远处传来急促的脚步声。有人正在被追赶，也可能正把危险带向你。";
            case "村庄" -> "你来到一处不知名的村庄。村民热情招待你，并表示愿意提供帮助。";
            case "人生意义" -> "短暂的安静里，你开始思考继续前进的理由。";
            case "好诗歪诗" -> "墙上写着几行难以理解的诗句。它们看起来不像诗，也不像警告。";
            default -> "无事发生。";
        };
    }

    /**
     * 执行指定事件。
     *
     * @param eventName 事件名称
     * @param optionIndex 选择的选项索引（从 0 开始）
     * @return 事件结果
     */
    public EventResult executeEvent(String eventName, int optionIndex) {
        return executeEvent(eventName, optionIndex, null);
    }

    public EventResult executeEvent(String eventName, int optionIndex, Player player) {
        EventResult dataResult = executeDataEvent(eventName, optionIndex);
        if (dataResult != null) {
            return dataResult;
        }
        switch (eventName) {
            case "命运抉择1": return firstDecision(optionIndex);
            case "命运抉择2": return secondDecision(optionIndex, player);
            case "相遇": return encounter(optionIndex);
            case "翅膀雕像": return wingStatue(optionIndex);
            case "黏液世界": return slimeWorld(optionIndex);
            case "投资": return investment(optionIndex);
            case "追猎": return hunt(optionIndex);
            case "村庄": return village(optionIndex);
            case "人生意义": return meaningOfLife(optionIndex);
            case "好诗歪诗": return poetry(optionIndex);
            default:
                return new EventResult("无事发生。", 0, 0, null);
        }
    }

    /**
     * 获取事件的选项列表。
     *
     * @param eventName 事件名称
     * @return 选项列表
     */
    public List<EventOption> getOptions(String eventName) {
        return getOptions(eventName, null);
    }

    public List<EventOption> getOptions(String eventName, Player player) {
        List<EventOption> dataOptions = dataOptions(eventName);
        if (!dataOptions.isEmpty()) {
            return dataOptions;
        }
        switch (eventName) {
            case "命运抉择1":
                return List.of(
                        new EventOption("前进", "突破迷雾，进入森林"),
                        new EventOption("回头", "结束游戏，获得隐藏结局「讲述中断」")
                );
            case "命运抉择2": {
                List<EventOption> options = new ArrayList<>();
                options.add(new EventOption("我要权力",
                        "绝对的集权，皇权至高无上。获得藏品「集权」"));
                options.add(new EventOption("我要财富",
                        "为了利益无所不用其极。获得藏品「寡头」"));
                if (canChooseBabelTower(player)) {
                    options.add(new EventOption("没有你，对我很重要",
                            "凡人联合起来对神的挑战。获得藏品「巴别塔」"));
                }
                return options;
            }
            case "相遇":
                return List.of(
                        new EventOption("✅ 同意", "同意同行，获得藏品「背叛」"),
                        new EventOption("❌ 拒绝", "拒绝同行，获得藏品「仇恨」"),
                        new EventOption("😶 无视", "假装没听见，转身离去")
                );
            case "翅膀雕像":
                return List.of(
                        new EventOption("🙏 祈祷", "从牌组移除一张牌，失去 7 生命值"),
                        new EventOption("🔨 摧毁", "获得 50-80 金币"),
                        new EventOption("🚶 离开", "无事发生")
                );
            case "黏液世界":
                return List.of(
                        new EventOption("💰 收集金币", "获得 75 金币，失去 11 生命值"),
                        new EventOption("🙌 放手吧", "失去 35-75 金币")
                );
            case "投资":
                return List.of(
                        new EventOption("0 金币", "投资 0 金币"),
                        new EventOption("50 金币", "投资 50 金币"),
                        new EventOption("100 金币", "投资 100 金币")
                );
            case "追猎":
                return List.of(
                        new EventOption("⚔️ 反抗", "失去 15 生命值，获得藏品「复仇者」"),
                        new EventOption("🏃 逃跑", "获得 20 生命值 + 一个负面藏品")
                );
            case "村庄":
                return List.of(
                        new EventOption("💣 曼哈顿计划", "获得 1 个核弹"),
                        new EventOption("💎 绿宝石", "获得 200 金币"),
                        new EventOption("🍔 板烧鸡腿堡", "回复生命值")
                );
            case "人生意义":
                return List.of(
                        new EventOption("🍟 去码头整点薯条", "生命值回满"),
                        new EventOption("🪙 去银行整点金条", "获得 500 金币")
                );
            case "好诗歪诗":
                return List.of(
                        new EventOption("📖 好诗", "获得 5 张牌"),
                        new EventOption("📝 歪诗", "获得随机 1 个价值 3 及以下藏品"),
                        new EventOption("🤔 看不懂思密达", "获得一个奇怪的藏品")
                );
            default:
                return List.of(new EventOption("离开", "无事发生"));
        }
    }

    private Map<String, DataLoader.EventData> loadEventsByName() {
        Map<String, DataLoader.EventData> events = new LinkedHashMap<>();
        for (DataLoader.EventData eventData : new DataLoader().loadEvents()) {
            if (eventData.getName() != null && !eventData.getName().isBlank()) {
                events.put(eventData.getName(), eventData);
            }
        }
        return events;
    }

    private List<EventOption> dataOptions(String eventName) {
        DataLoader.EventData eventData = eventsByName.get(eventName);
        if (eventData == null || eventData.getOptions().isEmpty()) {
            return List.of();
        }
        List<EventOption> options = new ArrayList<>();
        for (DataLoader.EventOptionData optionData : eventData.getOptions()) {
            options.add(new EventOption(optionData.getText(), optionData.getResult()));
        }
        return options;
    }

    private EventResult executeDataEvent(String eventName, int optionIndex) {
        DataLoader.EventData eventData = eventsByName.get(eventName);
        if (eventData == null || optionIndex < 0
                || optionIndex >= eventData.getOptions().size()) {
            return null;
        }
        DataLoader.EventOptionData optionData = eventData.getOptions().get(optionIndex);
        return EventResultResolver.resolve(optionData);
    }

    private List<String> hardcodedEventNames() {
        return List.of("相遇", "翅膀雕像", "黏液世界", "投资",
                "追猎", "村庄", "人生意义", "好诗歪诗");
    }

    // ==================== 具体事件实现 ====================

    private EventResult firstDecision(int option) {
        if (option == 1) {
            return new EventResult(
                    "你选择回头，离开这本荒诞的书。故事在此中断。",
                    0, 0, null, "ENDING_INTERRUPTED");
        }
        return new EventResult(
                "你拨开迷雾，继续向森林深处前进。",
                0, 0, null);
    }

    private EventResult secondDecision(int option, Player player) {
        if (option == 0) {
            return new EventResult(
                    "你接受了权力的许诺。\n获得藏品「集权」",
                    0, 0, "relic_centralization");
        }
        if (option == 1) {
            return new EventResult(
                    "你接受了财富的诱惑。\n获得藏品「寡头」",
                    0, 0, "relic_oligarch");
        }
        if (option == 2 && canChooseBabelTower(player)) {
            return new EventResult(
                    "你拒绝了他的交易，选择记住那座跨过语言、文化与阶级阻隔的塔。\n获得藏品「巴别塔」",
                    0, 0, "relic_babel_tower");
        }
        return new EventResult("你没有作出选择。", 0, 0, null);
    }

    private boolean canChooseBabelTower(Player player) {
        return player != null && (player.hasRelic("relic_betrayal")
                || player.hasRelic("relic_hatred"));
    }

    /**
     * 相遇事件。
     */
    private EventResult encounter(int option) {
        switch (option) {
            case 0: // 同意
                return new EventResult(
                        "你同意了同行的请求。他看起来不太对劲，但已经来不及反悔了。\n获得藏品「背叛」",
                        0, 0, "relic_betrayal");
            case 1: // 拒绝
                return new EventResult(
                        "你拒绝了同行的请求。他恶狠狠地看了你一眼，你不禁打了个寒颤。\n获得藏品「仇恨」",
                        0, 0, "relic_hatred");
            default: // 无视
                return new EventResult(
                        "你假装没听见，转身离去。身后传来一声若有若无的叹息。",
                        0, 0, null);
        }
    }

    /**
     * 翅膀雕像事件。
     */
    private EventResult wingStatue(int option) {
        switch (option) {
            case 0: // 祈祷
                return new EventResult(
                        "你虔诚地祈祷。雕像似乎在回应你，但你感到一阵眩晕。\n失去 7 点生命值",
                        0, -7, null);
            case 1: // 摧毁
                int gold = 50 + random.nextInt(31);
                return new EventResult(
                        "你用力砸碎了雕像，金币从裂缝中散落一地。\n获得 " + gold + " 金币",
                        gold, 0, null);
            default: // 离开
                return new EventResult("你决定不碰这座奇怪的雕像，转身离开。", 0, 0, null);
        }
    }

    /**
     * 黏液世界事件。
     */
    private EventResult slimeWorld(int option) {
        switch (option) {
            case 0: // 收集金币
                return new EventResult(
                        "你在黏液中摸索，找到了一些金币，但被腐蚀得不轻。\n获得 75 金币，失去 11 生命值",
                        75, -11, null);
            default: // 放手吧
                int lostGold = 35 + random.nextInt(41);
                return new EventResult(
                        "你挣扎着想脱身，但金币从口袋里掉了出来。\n失去 " + lostGold + " 金币",
                        -lostGold, 0, null);
        }
    }

    /**
     * 投资事件。
     */
    private EventResult investment(int option) {
        int roll = random.nextInt(100);
        switch (option) {
            case 0: // 投资 0
                if (roll < 25) {
                    return new EventResult("投资 0 金币：一无所获。", 0, 0, null);
                } else {
                    return new EventResult("投资 0 金币：白捡 10 金币。", 10, 0, null);
                }
            case 1: // 投资 50
                if (roll < 25) {
                    return new EventResult("投资 50 金币：血本无归。", -50, 0, null);
                } else if (roll < 75) {
                    return new EventResult("投资 50 金币：拿回本金。", 0, 0, null);
                } else {
                    return new EventResult("投资 50 金币：获得 100 金币！", 100, 0, null);
                }
            default: // 投资 100
                if (roll < 50) {
                    return new EventResult("投资 100 金币：血本无归。", -100, 0, null);
                } else if (roll < 75) {
                    return new EventResult("投资 100 金币：拿回本金。", 0, 0, null);
                } else if (roll < 95) {
                    return new EventResult("投资 100 金币：获利 150！", 150, 0, null);
                } else if (roll < 99) {
                    return new EventResult("投资 100 金币：大赚 200！", 200, 0, null);
                } else {
                    return new EventResult("投资 100 金币：暴富！获得 1000 金币！", 1000, 0, null);
                }
        }
    }

    /**
     * 追猎事件。
     */
    private EventResult hunt(int option) {
        switch (option) {
            case 0: // 反抗
                return new EventResult(
                        "你奋力还击，击退了黑暗中的生物。手上多了一件奇怪的武器。\n失去 15 生命值，获得藏品「复仇者」",
                        0, -15, "relic_avenger");
            default: // 逃跑
                return new EventResult(
                        "你拼命逃跑，虽然活了下来，但背上了一个奇怪的负担。\n获得一个负面藏品",
                        0, 20, "relic_curse_random");
        }
    }

    /**
     * 村庄事件。
     */
    private EventResult village(int option) {
        switch (option) {
            case 0: // 曼哈顿计划
                return new EventResult(
                        "村民给了你一个威力巨大的··· 等等，这是什么？\n获得 1 个核弹（特殊物品）",
                        0, 0, "relic_nuke");
            case 1: // 绿宝石
                return new EventResult(
                        "村民送给你一颗闪闪发光的绿宝石。\n获得 200 金币",
                        200, 0, null);
            default: // 板烧鸡腿堡
                int heal = 15 + random.nextInt(16);
                return new EventResult(
                        "美味的板烧鸡腿堡！你感到精力充沛。\n回复 " + heal + " 生命值",
                        0, heal, null);
        }
    }

    /**
     * 人生意义事件。
     */
    private EventResult meaningOfLife(int option) {
        switch (option) {
            case 0: // 薯条
                return new EventResult(
                        "你听取了精灵的建议，去码头整了点薯条。人生嘛，有时候就是这么简单。\n生命值回满",
                        0, 9999, null);
            default: // 金条
                return new EventResult(
                        "你听取了精灵的建议，去银行整了点金条。\n获得 500 金币",
                        500, 0, null);
        }
    }

    /**
     * 好诗歪诗事件。
     */
    private EventResult poetry(int option) {
        switch (option) {
            case 0: // 好诗
                return new EventResult(
                        "你在这首诗前驻足良久，心中涌现出无限灵感。\n获得 5 张牌",
                        0, 0, "relic_five_cards");
            case 1: // 歪诗
                return new EventResult(
                        "这首诗··· 工整吗？不管了，你发现墙后有东西。\n获得一个藏品",
                        0, 0, "relic_random_leq3");
            default: // 看不懂
                return new EventResult(
                        "你看不懂，但大为震撼。似乎有什么东西缠上了你。\n获得一个奇怪（负面）的藏品",
                        0, 0, "relic_curse_random");
        }
    }
}
