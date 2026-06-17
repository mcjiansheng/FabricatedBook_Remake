package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.fabricatedbook.core.run.GameRunState;

/**
 * NodeFactory — 节点工厂（权重随机算法）
 * <p>
 * 根据 MapConfig 中的权重概率表创建地图节点。
 * 使用权重随机算法：P(Node) = Weight(Target) / Σ(Weight(All))。
 * <p>
 * 引用方：MapGraph（生成地图时调用）
 */
public class NodeFactory {

    private static final Random RANDOM = new Random();

    /**
     * 根据配置生成地图的所有节点。
     * <p>
     * 起点和终点使用指定类型，其余节点按权重随机生成。
     *
     * @param config 地图配置
     * @return 生成的节点列表（按列优先排列）
     */
    public static List<List<Node>> generateNodes(MapConfig config) {
        return generateNodes(config, RANDOM);
    }

    public static List<List<Node>> generateNodes(MapConfig config, Random random) {
        int width = config.getWidth();
        int height = config.getHeight();
        List<List<Node>> grid = new ArrayList<>();
        Random rng = random == null ? RANDOM : random;

        for (int row = 0; row < height; row++) {
            List<Node> rowNodes = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                Node node;
                if (row == 0 && col == config.getStartCol()) {
                    node = new Node(config.getStartNodeType(), row, col);
                } else if (row == config.getEndRow() && config.getEndNodeType() != null) {
                    if (col == width / 2) {
                        node = new Node(config.getEndNodeType(), row, col);
                    } else {
                        node = new Node(randomNodeType(config.getNodeWeights(), rng), row, col);
                    }
                } else {
                    NodeType type = randomNodeType(config.getNodeWeights(), rng);
                    node = new Node(type, row, col);
                }
                rowNodes.add(node);
            }
            grid.add(rowNodes);
        }

        return grid;
    }

    public static List<List<Node>> generateNodes(MapConfig config, long seed,
                                                 String keyPrefix) {
        int width = config.getWidth();
        int height = config.getHeight();
        List<List<Node>> grid = new ArrayList<>();
        String prefix = keyPrefix == null ? "map" : keyPrefix;

        for (int row = 0; row < height; row++) {
            List<Node> rowNodes = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                Node node;
                if (row == 0 && col == config.getStartCol()) {
                    node = new Node(config.getStartNodeType(), row, col);
                } else if (row == config.getEndRow() && config.getEndNodeType() != null) {
                    if (col == width / 2) {
                        node = new Node(config.getEndNodeType(), row, col);
                    } else {
                        node = new Node(randomNodeType(config.getNodeWeights(),
                                GameRunState.randomFor(seed, prefix + ":node:" + row + ":" + col)), row, col);
                    }
                } else {
                    NodeType type = randomNodeType(config.getNodeWeights(),
                            GameRunState.randomFor(seed, prefix + ":node:" + row + ":" + col));
                    node = new Node(type, row, col);
                }
                rowNodes.add(node);
            }
            grid.add(rowNodes);
        }

        return grid;
    }

    /**
     * 权重随机算法 — 根据权重表选择一个节点类型。
     *
     * @param weights 节点类型 -> 权重映射
     * @return 选中的节点类型
     */
    public static NodeType randomNodeType(Map<NodeType, Integer> weights) {
        return randomNodeType(weights, RANDOM);
    }

    public static NodeType randomNodeType(Map<NodeType, Integer> weights, Random random) {
        if (weights == null || weights.isEmpty()) {
            return NodeType.FIGHT;
        }

        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return NodeType.FIGHT;
        }

        Random rng = random == null ? RANDOM : random;
        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;

        for (Map.Entry<NodeType, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        return NodeType.FIGHT; // fallback
    }

    /**
     * 创建单个指定类型的节点。
     *
     * @param type 节点类型
     * @param row  行号
     * @param col  列号
     * @return 节点实例
     */
    public static Node createNode(NodeType type, int row, int col) {
        return new Node(type, row, col);
    }
}
