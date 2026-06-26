package com.fabricatedbook.core.map;

import com.fabricatedbook.core.run.GameRunState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sparse column-first map graph shared by frontend map rendering and backend CLI.
 */
public class LayerMapGraph {
    private final LayerMapConfig config;
    private final long seed;
    private final int layerIndex;
    private final LayerMapNode[][] columns;
    private final int[] nodeCounts;
    private LayerMapNode currentNode;

    public LayerMapGraph(LayerMapConfig config, long seed, int layerIndex) {
        this.config = config;
        this.seed = seed;
        this.layerIndex = layerIndex;
        this.nodeCounts = generateNodeCounts();
        this.columns = generateNodes();
        connectNodes();
    }

    private int[] generateNodeCounts() {
        int[] counts = new int[config.getLength()];
        counts[0] = 1;
        counts[config.getLength() - 1] = 1;
        for (int col = 1; col < config.getLength() - 1; col++) {
            counts[col] = 1 + randomFor("map-line-count", col).nextInt(config.getWidth());
        }
        if (config.hasSpecialBossColumn()) {
            counts[config.getSpecialBossColumn()] = 1;
        }
        return counts;
    }

    private LayerMapNode[][] generateNodes() {
        LayerMapNode[][] generated = new LayerMapNode[config.getLength()][];
        for (int col = 0; col < config.getLength(); col++) {
            generated[col] = new LayerMapNode[nodeCounts[col]];
            for (int row = 0; row < nodeCounts[col]; row++) {
                NodeType type;
                if (col == 0) {
                    type = config.getStartType();
                } else if (col == config.getLength() - 1) {
                    type = config.getEndType();
                } else if (col == config.getSpecialBossColumn()) {
                    type = NodeType.BOSS;
                } else {
                    type = randomChoice(config.getProbabilities(),
                            randomFor("map-node-type", col, row));
                }
                generated[col][row] = new LayerMapNode(col, row, type);
            }
        }
        return generated;
    }

    private void connectNodes() {
        int lastCol = config.getLength() - 1;
        int tail = config.hasSpecialBossColumn()
                ? config.getSpecialBossColumn() - 1
                : lastCol - 1;

        for (int col = 0; col < lastCol; col++) {
            if (col == 0) {
                for (LayerMapNode node : columns[1]) {
                    columns[0][0].addNext(node);
                }
            } else if (col < tail) {
                connectColumns(col);
            } else if (col == tail) {
                LayerMapNode target = config.hasSpecialBossColumn()
                        ? columns[config.getSpecialBossColumn()][0]
                        : columns[lastCol][0];
                for (LayerMapNode node : columns[col]) {
                    node.addNext(target);
                }
            } else if (config.hasSpecialBossColumn()
                    && col == config.getSpecialBossColumn()) {
                columns[col][0].addNext(columns[lastCol][0]);
            }
        }
    }

    private void connectColumns(int col) {
        int x = 0;
        int y = 0;
        for (int row = 0; row < nodeCounts[col]; row++) {
            int size = Math.max(0, nodeCounts[col + 1] - 1 - y);
            if (size > 0) {
                y += randomFor("map-connection", col, row).nextInt(size + 1);
            }
            if (row == nodeCounts[col] - 1 && y < nodeCounts[col + 1] - 1) {
                y = nodeCounts[col + 1] - 1;
            }
            for (int targetRow = x; targetRow <= y; targetRow++) {
                columns[col][row].addNext(columns[col + 1][targetRow]);
            }
            x = y;
        }
    }

    private Random randomFor(String purpose, int... parts) {
        StringBuilder key = new StringBuilder("map");
        key.append(':').append(layerIndex).append(':').append(purpose);
        for (int part : parts) {
            key.append(':').append(part);
        }
        return GameRunState.randomFor(seed, key.toString());
    }

    private static NodeType randomChoice(int[] probabilities, Random random) {
        int total = 0;
        for (int probability : probabilities) total += probability;
        if (total <= 0) return NodeType.FIGHT;
        int roll = random.nextInt(total);
        int sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (roll < sum) return fromTypeCode(i + 1);
        }
        return NodeType.FIGHT;
    }

    public static NodeType fromTypeCode(int typeCode) {
        switch (typeCode) {
            case 1: return NodeType.FIGHT;
            case 2: return NodeType.EMERGENCY;
            case 3: return NodeType.BOSS;
            case 4: return NodeType.UNEXPECTED;
            case 5: return NodeType.REWARD;
            case 6: return NodeType.SHOP;
            case 8: return NodeType.DECISION;
            case 9: return NodeType.SAFEHOUSE;
            default: return NodeType.FIGHT;
        }
    }

    public List<LayerMapNode> getAvailableNodes() {
        if (currentNode == null) {
            return List.of(columns[0][0]);
        }
        List<LayerMapNode> available = new ArrayList<>();
        for (LayerMapNode node : currentNode.getNext()) {
            if (!node.isVisited()) {
                available.add(node);
            }
        }
        return available;
    }

    public boolean moveTo(LayerMapNode node) {
        if (node == null) return false;
        if (!getAvailableNodes().contains(node)) return false;
        currentNode = node;
        currentNode.setVisited(true);
        return true;
    }

    public boolean restorePosition(int col, int row) {
        LayerMapNode node = getNode(col, row);
        if (node == null) return false;
        currentNode = node;
        currentNode.setVisited(true);
        return true;
    }

    public LayerMapNode getNode(int col, int row) {
        if (col < 0 || col >= columns.length) return null;
        if (row < 0 || row >= columns[col].length) return null;
        return columns[col][row];
    }

    public LayerMapConfig getConfig() { return config; }
    public LayerMapNode[][] getColumns() { return columns; }
    public int[] getNodeCounts() { return nodeCounts.clone(); }
    public LayerMapNode getCurrentNode() { return currentNode; }
    public boolean isAtEnd() {
        return currentNode != null && currentNode.getCol() >= columns.length - 1;
    }
}
