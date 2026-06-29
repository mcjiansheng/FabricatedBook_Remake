package com.fabricatedbook.data;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.map.LayerMapConfig;
import com.fabricatedbook.core.map.MapConfig;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.RelicData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * DataLoader — 数据加载器
 * <p>
 * 从 JSON 文件读取卡牌、怪物、藏品、地图配置等数据。
 * 使用 Gson 库进行 JSON 反序列化。
 * 支持从文件系统和类路径两种方式加载。
 * <p>
 * 引用方：GameController（启动时加载所有配置）、
 *         CardPool（注册卡牌）、EntityFactory（创建敌人）
 */
public class DataLoader {

    /** Gson 实例（带格式化输出） */
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    /** 数据根目录 */
    private String dataPath;

    /**
     * 构造数据加载器。
     *
     * @param dataPath 数据文件根目录路径
     */
    public DataLoader(String dataPath) {
        this.dataPath = dataPath;
    }

    /**
     * 默认构造：数据目录为 "data/"。
     */
    public DataLoader() {
        this("data/");
    }

    /**
     * 从 JSON 文件加载卡牌数据。
     *
     * @param profession 职业标识（如 "warrior"）
     * @return 卡牌列表
     */
    public List<Card> loadCards(String profession) {
        String path = dataPath + "cards/" + profession + ".json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<Card>>() {}.getType();
            List<Card> cards = gson.fromJson(json, listType);
            return cards != null ? cards : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载卡牌失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从 JSON 文件加载怪物数据。
     *
     * @param level 楼层编号（1-5）
     * @return 怪物数据列表（每个元素是一个怪物组）
     */
    public List<EnemyGroup> loadMonsters(int level) {
        String path = dataPath + "monsters/level" + level + ".json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<EnemyGroup>>() {}.getType();
            List<EnemyGroup> groups = gson.fromJson(json, listType);
            return groups != null ? groups : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载怪物失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从 JSON 文件加载藏品数据。
     *
     * @return 藏品列表
     */
    public List<RelicData> loadRelicData() {
        String path = dataPath + "relics.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<RelicData>>() {}.getType();
            List<RelicData> relics = gson.fromJson(json, listType);
            return relics != null ? relics : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载藏品失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Potion> loadPotions() {
        String path = dataPath + "potions.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<Potion>>() {}.getType();
            List<Potion> potions = gson.fromJson(json, listType);
            return potions != null ? potions : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载药水失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<EventData> loadEvents() {
        String path = dataPath + "events.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<EventData>>() {}.getType();
            List<EventData> events = gson.fromJson(json, listType);
            return events != null ? events : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载事件失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从 JSON 文件加载地图配置。
     *
     * @return 地图配置列表（5 层）
     */
    public List<MapConfig> loadMapConfigs() {
        List<MapConfig> configs = new ArrayList<>();
        for (LayerMapConfig layer : loadLayerMapConfigs()) {
            configs.add(new MapConfig(layer.getLength(), layer.getWidth(),
                    layer.getStartType(), layer.getEndType(),
                    probabilityMap(layer.getProbabilities()),
                    layer.getEffectText(), layer.getLevel()));
        }
        return configs.isEmpty() ? defaultMapConfigs() : configs;
    }

    public List<LayerMapConfig> loadLayerMapConfigs() {
        String path = dataPath + "maps/levels.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<LayerMapData>>() {}.getType();
            List<LayerMapData> data = gson.fromJson(json, listType);
            List<LayerMapConfig> configs = new ArrayList<>();
            if (data != null) {
                for (LayerMapData layer : data) {
                    LayerMapConfig config = layer.toConfig();
                    if (config != null) {
                        configs.add(config);
                    }
                }
            }
            return configs.isEmpty() ? LayerMapConfig.defaults() : configs;
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载稀疏地图配置失败: " + path + " - " + e.getMessage());
            return LayerMapConfig.defaults();
        }
    }

    /**
     * 获取默认地图配置（当 JSON 加载失败时使用）。
     *
     * @return 5 层默认地图配置
     */
    public static List<MapConfig> defaultMapConfigs() {
        return List.of(
                MapConfig.wilderness(),
                MapConfig.forest(),
                MapConfig.mysticForest(),
                MapConfig.mist(),
                MapConfig.tower()
        );
    }

    private Map<NodeType, Integer> probabilityMap(int[] probabilities) {
        Map<NodeType, Integer> weights = new EnumMap<>(NodeType.class);
        for (int i = 0; i < probabilities.length; i++) {
            int weight = probabilities[i];
            if (weight <= 0) continue;
            NodeType type = nodeTypeFromCode(i + 1);
            if (type != null) {
                weights.put(type, weight);
            }
        }
        return weights;
    }

    private static NodeType nodeTypeFromCode(int typeCode) {
        return switch (typeCode) {
            case 1 -> NodeType.FIGHT;
            case 2 -> NodeType.EMERGENCY;
            case 3 -> NodeType.BOSS;
            case 4 -> NodeType.UNEXPECTED;
            case 5 -> NodeType.REWARD;
            case 6 -> NodeType.SHOP;
            case 8 -> NodeType.DECISION;
            case 9 -> NodeType.SAFEHOUSE;
            default -> null;
        };
    }

    public static class EventData {
        private String id;
        private String name;
        private String description;
        private Boolean randomPool;
        private List<EventOptionData> options;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isRandomPool() { return randomPool == null || randomPool; }
        public List<EventOptionData> getOptions() {
            return options != null ? options : List.of();
        }
    }

    public static class EventOptionData {
        private String text;
        private String result;
        private String outcomeDescription;
        private Integer goldChange;
        private Integer goldChangeMin;
        private Integer goldChangeMax;
        private Integer hpChange;
        private Integer hpChangeMin;
        private Integer hpChangeMax;
        private boolean fullHeal;
        private String relicId;
        private String outcome;
        private List<EventOutcomeData> randomOutcomes;
        private String executor;
        private List<String> requiresAnyRelic;

        public String getText() { return text; }
        public String getResult() { return result; }
        public String getOutcomeDescription() { return outcomeDescription; }
        public int getGoldChange() { return goldChange != null ? goldChange : 0; }
        public Integer getGoldChangeMin() { return goldChangeMin; }
        public Integer getGoldChangeMax() { return goldChangeMax; }
        public boolean hasRandomGoldChange() {
            return goldChangeMin != null && goldChangeMax != null;
        }
        public int getHpChange() { return hpChange != null ? hpChange : 0; }
        public Integer getHpChangeMin() { return hpChangeMin; }
        public Integer getHpChangeMax() { return hpChangeMax; }
        public boolean hasRandomHpChange() {
            return hpChangeMin != null && hpChangeMax != null;
        }
        public boolean isFullHeal() { return fullHeal; }
        public String getRelicId() { return relicId; }
        public String getOutcome() { return outcome; }
        public List<EventOutcomeData> getRandomOutcomes() {
            return randomOutcomes != null ? randomOutcomes : List.of();
        }
        public boolean hasRandomOutcomes() {
            return randomOutcomes != null && !randomOutcomes.isEmpty();
        }
        public String getExecutor() { return executor; }
        public List<String> getRequiresAnyRelic() {
            return requiresAnyRelic != null ? requiresAnyRelic : List.of();
        }
        public boolean hasExecutableResult() {
            return (outcomeDescription != null && !outcomeDescription.isBlank())
                    || hasRandomOutcomes();
        }
        public boolean usesJavaExecutor() {
            return "java".equalsIgnoreCase(executor);
        }
    }

    public static class EventOutcomeData {
        private int weight;
        private String outcomeDescription;
        private Integer goldChange;
        private Integer hpChange;
        private boolean fullHeal;
        private String relicId;
        private String outcome;

        public int getWeight() { return weight; }
        public String getOutcomeDescription() { return outcomeDescription; }
        public int getGoldChange() { return goldChange != null ? goldChange : 0; }
        public int getHpChange() { return hpChange != null ? hpChange : 0; }
        public boolean isFullHeal() { return fullHeal; }
        public String getRelicId() { return relicId; }
        public String getOutcome() { return outcome; }
    }

    public static class LayerMapData {
        private int level;
        private String name;
        private String effectText;
        private int length;
        private int width;
        private NodeType startType;
        private NodeType endType;
        private Map<NodeType, Integer> probabilities;
        private int specialBossColumn = -1;

        LayerMapConfig toConfig() {
            if (level <= 0 || length <= 0 || width <= 0
                    || startType == null || endType == null) {
                return null;
            }
            return new LayerMapConfig(level, name, effectText, length, width,
                    startType, endType, toProbabilityArray(probabilities),
                    specialBossColumn);
        }

        private int[] toProbabilityArray(Map<NodeType, Integer> weights) {
            int[] values = new int[9];
            if (weights == null) {
                return values;
            }
            for (Map.Entry<NodeType, Integer> entry : weights.entrySet()) {
                int index = typeCode(entry.getKey()) - 1;
                if (index >= 0 && index < values.length) {
                    values[index] = Math.max(0, entry.getValue());
                }
            }
            return values;
        }

        private int typeCode(NodeType type) {
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
    }

    // ==================== 辅助方法 ====================

    /**
     * 将文件内容读取为字符串。
     *
     * @param path 文件路径
     * @return 文件内容
     */
    private String readFileAsString(String path) {
        // 尝试从文件系统读取
        File file = new File(path);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int len;
                while ((len = reader.read(buf)) != -1) {
                    sb.append(buf, 0, len);
                }
                return sb.toString();
            } catch (Exception e) {
                System.err.println("[DataLoader] 读取文件失败: " + path);
            }
        }

        // 尝试从类路径读取
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int len;
                    while ((len = reader.read(buf)) != -1) {
                        sb.append(buf, 0, len);
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("[DataLoader] 从类路径加载失败: " + path);
        }

        return "[]";
    }

    /**
     * 设置数据路径。
     *
     * @param dataPath 数据目录路径
     */
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    // ==================== 内部数据结构 ====================

    /**
     * 怪物组：包含同一场战斗中的多个敌人及其行动脚本。
     */
    public static class EnemyGroup {
        private String id;
        private String name;
        private List<EnemyData> enemies;
        private boolean isBoss;
        private int floor;

        public String getId() { return id; }
        public String getName() { return name; }
        public List<EnemyData> getEnemies() { return enemies; }
        public boolean isBoss() { return isBoss; }
        public int getFloor() { return floor; }
    }

    /**
     * 怪物数据（JSON 反序列化用）。
     */
    public static class EnemyData {
        private String id;
        private String name;
        private int maxHp;
        private List<String> actionScript;
        private String passive;

        public String getId() { return id; }
        public String getName() { return name; }
        public int getMaxHp() { return maxHp; }
        public List<String> getActionScript() { return actionScript; }
        public String getPassive() { return passive; }

        public Enemy toEnemy() {
            return new Enemy(id, name, maxHp, actionScript, passive);
        }
    }
}
