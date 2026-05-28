package com.fabricatedbook.core.map;

import java.util.HashMap;
import java.util.Map;

/**
 * MapConfig — 地图区域配置
 * <p>
 * 包含地图的尺寸、起点/终点节点类型、各节点类型的权重概率表和环境效果。
 * 每层地图（荒野/森林/诡异秘林/迷雾/高塔）都有独立的配置。
 * <p>
 * 数据来源：game_encyclopedia.md "二、地图系统"
 * 引用方：NodeFactory（按权重创建节点）、MapGraph（生成地图）
 */
public class MapConfig {

    /** 地图宽度（列数） */
    private final int width;

    /** 地图高度（行数） */
    private final int height;

    /** 起点节点类型 */
    private final NodeType startNodeType;

    /** 终点节点类型 */
    private final NodeType endNodeType;

    /** 各节点类型的权重概率表 */
    private final Map<NodeType, Integer> nodeWeights;

    /** 环境效果描述 */
    private final String environmentEffect;

    /** 层级编号（1-5） */
    private final int level;

    /** 起点固定列 */
    private final int startCol;

    /** 终点固定行 */
    private final int endRow;

    /**
     * 构造地图配置。
     *
     * @param width             地图宽度（列数）
     * @param height            地图高度（行数）
     * @param startNodeType     起点类型
     * @param endNodeType       终点类型
     * @param nodeWeights       节点权重表
     * @param environmentEffect 环境效果
     * @param level             层级编号
     */
    public MapConfig(int width, int height, NodeType startNodeType,
                     NodeType endNodeType, Map<NodeType, Integer> nodeWeights,
                     String environmentEffect, int level) {
        this.width = width;
        this.height = height;
        this.startNodeType = startNodeType;
        this.endNodeType = endNodeType;
        this.nodeWeights = nodeWeights;
        this.environmentEffect = environmentEffect;
        this.level = level;
        this.startCol = width / 2; // 起点居中
        this.endRow = height - 1;  // 终点在最后一行
    }

    // ====== 预配置的五层地图 ======

    /**
     * 获取第 1 层：荒野 配置。
     * 尺寸 4×3，起点战斗，终点命运抉择，无环境效果。
     */
    public static MapConfig wilderness() {
        Map<NodeType, Integer> weights = new HashMap<>();
        weights.put(NodeType.FIGHT, 60);
        weights.put(NodeType.EMERGENCY, 10);
        weights.put(NodeType.UNEXPECTED, 30);
        return new MapConfig(4, 3, NodeType.FIGHT, NodeType.DECISION,
                weights, "无", 1);
    }

    /**
     * 获取第 2 层：森林 配置。
     * 尺寸 5×3，起点战斗，终点商店。
     * 环境效果：非战斗节点损失金币，战斗获得金币增加。
     */
    public static MapConfig forest() {
        Map<NodeType, Integer> weights = new HashMap<>();
        weights.put(NodeType.FIGHT, 40);
        weights.put(NodeType.EMERGENCY, 20);
        weights.put(NodeType.UNEXPECTED, 10);
        weights.put(NodeType.SAFEHOUSE, 10);
        weights.put(NodeType.SHOP, 10);
        weights.put(NodeType.REWARD, 10);
        return new MapConfig(5, 3, NodeType.FIGHT, NodeType.SHOP,
                weights, "非战斗节点损失金币，战斗获得金币增加", 2);
    }

    /**
     * 获取第 3 层：诡异秘林 配置。
     * 尺寸 6×4，起点得偿所愿，终点 Boss 战。
     * 环境效果：进战斗伤害 -1，进非战斗伤害 +1（±3）。
     */
    public static MapConfig mysticForest() {
        Map<NodeType, Integer> weights = new HashMap<>();
        weights.put(NodeType.FIGHT, 50);
        weights.put(NodeType.EMERGENCY, 20);
        weights.put(NodeType.UNEXPECTED, 15);
        weights.put(NodeType.SAFEHOUSE, 5);
        weights.put(NodeType.REWARD, 5);
        weights.put(NodeType.SHOP, 5);
        return new MapConfig(6, 4, NodeType.REWARD, NodeType.BOSS,
                weights, "进战斗伤害-1，进非战斗伤害+1(±3)", 3);
    }

    /**
     * 获取第 4 层：迷雾 配置。
     * 尺寸 7×4，起点紧急作战，终点 Boss → 命运抉择。
     * 环境效果：每次前进概率回血或扣血。
     */
    public static MapConfig mist() {
        Map<NodeType, Integer> weights = new HashMap<>();
        weights.put(NodeType.FIGHT, 40);
        weights.put(NodeType.EMERGENCY, 20);
        weights.put(NodeType.UNEXPECTED, 20);
        weights.put(NodeType.SAFEHOUSE, 10);
        weights.put(NodeType.REWARD, 5);
        weights.put(NodeType.SHOP, 5);
        return new MapConfig(7, 4, NodeType.EMERGENCY, NodeType.BOSS,
                weights, "每次前进概率回血或扣血", 4);
    }

    /**
     * 获取第 5 层：高塔 配置。
     * 尺寸 7×4，起点不期而遇，终点 Boss 战，无环境效果。
     */
    public static MapConfig tower() {
        Map<NodeType, Integer> weights = new HashMap<>();
        weights.put(NodeType.FIGHT, 40);
        weights.put(NodeType.EMERGENCY, 20);
        weights.put(NodeType.UNEXPECTED, 10);
        weights.put(NodeType.SAFEHOUSE, 10);
        weights.put(NodeType.SHOP, 10);
        weights.put(NodeType.REWARD, 10);
        return new MapConfig(7, 4, NodeType.UNEXPECTED, NodeType.BOSS,
                weights, "无", 5);
    }

    // ====== Getter ======

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLevel() { return level; }

    public NodeType getStartNodeType() { return startNodeType; }
    public NodeType getEndNodeType() { return endNodeType; }

    public int getStartCol() { return startCol; }
    public int getEndRow() { return endRow; }

    public Map<NodeType, Integer> getNodeWeights() { return nodeWeights; }

    public String getEnvironmentEffect() { return environmentEffect; }

    /**
     * 获取该层地图的名称。
     *
     * @return 中文名称
     */
    public String getLevelName() {
        switch (level) {
            case 1: return "荒野";
            case 2: return "森林";
            case 3: return "诡异秘林";
            case 4: return "迷雾";
            case 5: return "高塔";
            default: return "未知";
        }
    }
}
