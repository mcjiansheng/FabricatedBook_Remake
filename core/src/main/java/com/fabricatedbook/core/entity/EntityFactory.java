package com.fabricatedbook.core.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityFactory — 实体工厂
 * <p>
 * 从 JSON 数据文件创建实体实例。
 * 支持创建 Player 和 Enemy 实例。
 * JSON 数据格式参考 data/monsters/ 目录下的文件。
 * <p>
 * 引用方：DataLoader（加载怪物数据）、CombatEngine（创建战斗实体）
 */
public class EntityFactory {

    /**
     * 创建玩家实体。
     *
     * @param playerId   玩家 ID
     * @param playerName 玩家名称
     * @param profession 职业
     * @return 玩家实例
     */
    public static Player createPlayer(String playerId, String playerName,
                                      Profession profession) {
        return new Player(playerId, playerName, profession);
    }

    /**
     * 从 JSON 文件路径创建敌人列表。
     *
     * @param filePath JSON 文件路径（类路径或文件系统路径）
     * @return 敌人列表
     */
    public static List<Enemy> createEnemiesFromJson(String filePath) {
        List<Enemy> enemies = new ArrayList<>();
        try {
            JsonArray jsonArray;
            // 尝试从类路径加载
            InputStream is = EntityFactory.class.getResourceAsStream(filePath);
            if (is != null) {
                jsonArray = JsonParser.parseReader(new InputStreamReader(is))
                        .getAsJsonArray();
            } else {
                // 从文件系统加载
                jsonArray = JsonParser.parseReader(new FileReader(filePath))
                        .getAsJsonArray();
            }

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                int hp = obj.get("hp").getAsInt();

                List<String> actions = new ArrayList<>();
                if (obj.has("actions")) {
                    JsonArray actionsArray = obj.get("actions").getAsJsonArray();
                    for (JsonElement action : actionsArray) {
                        actions.add(action.getAsString());
                    }
                }

                Enemy enemy = new Enemy(id, name, hp, actions);
                enemies.add(enemy);
            }
        } catch (Exception e) {
            System.err.println("EntityFactory: 加载怪物 JSON 失败: " + filePath);
            e.printStackTrace();
        }
        return enemies;
    }

    /**
     * 创建一个简单的敌人（用于测试或快速生成）。
     *
     * @param id   怪物 ID
     * @param name 怪物名称
     * @param hp   生命值
     * @return 敌人实例
     */
    public static Enemy createSimpleEnemy(String id, String name, int hp) {
        List<String> actions = new ArrayList<>();
        actions.add("atk1");
        return new Enemy(id, name, hp, actions);
    }
}
