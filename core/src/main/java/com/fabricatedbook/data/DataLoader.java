package com.fabricatedbook.data;

import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.map.MapConfig;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.relic.Relic;
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
import java.util.List;

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
    public List<Relic> loadRelics() {
        String path = dataPath + "relics.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<Relic>>() {}.getType();
            List<Relic> relics = gson.fromJson(json, listType);
            return relics != null ? relics : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载藏品失败: " + path + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从 JSON 文件加载地图配置。
     *
     * @return 地图配置列表（5 层）
     */
    public List<MapConfig> loadMapConfigs() {
        String path = dataPath + "maps/levels.json";
        try {
            String json = readFileAsString(path);
            Type listType = new TypeToken<List<MapConfig>>() {}.getType();
            List<MapConfig> configs = gson.fromJson(json, listType);
            return configs != null ? configs : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataLoader] 加载地图配置失败: " + path + " - " + e.getMessage());
            // 返回默认配置
            return defaultMapConfigs();
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
            return new Enemy(id, name, maxHp, actionScript);
        }
    }
}
