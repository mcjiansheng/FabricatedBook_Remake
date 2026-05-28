package com.fabricatedbook.core.map;

import java.util.*;

/**
 * MapGraph — 地图图结构（邻接表）
 * <p>
 * 管理地图的节点网格和节点间的连接关系。
 * 支持生成完整的地图路径、可视性控制和玩家位置追踪。
 * 使用邻接表表示图的连接关系。
 * <p>
 * 引用方：CombatEngine（玩家移动时更新状态）、
 *         MapScreen（渲染地图）、
 *         MapConfig（初始化配置）
 */
public class MapGraph {

    /** 节点网格 grid[row][col] */
    private final List<List<Node>> grid;

    /** 地图配置 */
    private final MapConfig config;

    /** 玩家当前位置（节点引用） */
    private Node playerPosition;

    /** 玩家当前所在行 */
    private int currentRow;

    /** 随机数生成器 */
    private final Random random;

    /**
     * 构造地图图结构。
     *
     * @param config 地图配置
     */
    public MapGraph(MapConfig config) {
        this.config = config;
        this.grid = NodeFactory.generateNodes(config);
        this.random = new Random();
        this.currentRow = 0;

        // 生成节点间的连接关系
        generateConnections();

        // 设置起始可见性
        applyInitialVisibility();
    }

    /**
     * 生成节点间的连接关系。
     * <p>
     * 每行节点与下一行之间的连接规则：
     * - 每个节点连接到下一行的相邻列（col-1, col, col+1）
     * - 如果 col 在边界，则只连接到合法范围
     */
    private void generateConnections() {
        for (int row = 0; row < grid.size() - 1; row++) {
            List<Node> currentRow = grid.get(row);
            List<Node> nextRow = grid.get(row + 1);

            for (int col = 0; col < currentRow.size(); col++) {
                Node node = currentRow.get(col);
                // 连接到下一行的相邻列
                for (int dCol = -1; dCol <= 1; dCol++) {
                    int targetCol = col + dCol;
                    if (targetCol >= 0 && targetCol < nextRow.size()) {
                        node.addConnection(nextRow.get(targetCol));
                    }
                }
            }
        }

        // 设置玩家起始节点
        List<Node> startRow = grid.get(0);
        playerPosition = startRow.get(config.getStartCol());
        playerPosition.setVisited(true);
        playerPosition.setEnabled(true);
    }

    /**
     * 设置初始可见性：第一行节点可见。
     */
    private void applyInitialVisibility() {
        if (!grid.isEmpty()) {
            List<Node> firstRow = grid.get(0);
            for (Node node : firstRow) {
                node.setVisible(true);
                node.setEnabled(true);
            }
        }
        // 使下一行节点可见
        if (grid.size() > 1) {
            for (Node node : grid.get(1)) {
                node.setVisible(true);
            }
        }
    }

    /**
     * 获取当前行可选择的节点列表。
     *
     * @return 当前行中已启用且未被访问的节点
     */
    public List<Node> getAvailableNodes() {
        if (currentRow >= grid.size()) return new ArrayList<>();

        List<Node> available = new ArrayList<>();
        if (playerPosition != null) {
            for (Node connection : playerPosition.getConnections()) {
                if (!connection.isVisited()) {
                    available.add(connection);
                }
            }
        }
        return available;
    }

    /**
     * 玩家移动到指定节点。
     *
     * @param node 目标节点
     * @return true 如果移动成功
     */
    public boolean moveTo(Node node) {
        if (node == null) return false;

        // 检查是否为当前节点的可达节点
        if (playerPosition == null || !playerPosition.getConnections().contains(node)) {
            return false;
        }

        playerPosition = node;
        node.setVisited(true);
        currentRow = node.getRow();

        // 使下一行节点可见
        if (currentRow + 1 < grid.size()) {
            for (Node nextNode : grid.get(currentRow + 1)) {
                nextNode.setVisible(true);
            }
        }

        // 使用可达节点
        applyEnablement();

        return true;
    }

    /**
     * 更新下一行中可达节点的启用状态。
     */
    private void applyEnablement() {
        if (currentRow + 1 < grid.size()) {
            Set<Node> reachable = new HashSet<>();
            if (playerPosition != null) {
                reachable.addAll(playerPosition.getConnections());
            }
            for (Node node : grid.get(currentRow + 1)) {
                node.setEnabled(reachable.contains(node));
            }
        }
    }

    // ====== 查询方法 ======

    /**
     * 获取整个节点网格。
     *
     * @return 节点网格
     */
    public List<List<Node>> getGrid() {
        return grid;
    }

    /**
     * 获取指定行的节点列表。
     *
     * @param row 行号
     * @return 节点列表
     */
    public List<Node> getRow(int row) {
        if (row >= 0 && row < grid.size()) {
            return grid.get(row);
        }
        return new ArrayList<>();
    }

    /**
     * 获取地图配置。
     *
     * @return 配置
     */
    public MapConfig getConfig() {
        return config;
    }

    /**
     * 获取玩家当前位置。
     *
     * @return 当前节点
     */
    public Node getPlayerPosition() {
        return playerPosition;
    }

    /**
     * 获取玩家当前行号。
     *
     * @return 行号
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * 判断是否已到达地图终点。
     *
     * @return true 如果玩家已在最后一行
     */
    public boolean isAtEnd() {
        return currentRow >= grid.size() - 1;
    }

    /**
     * 获取地图总行数。
     *
     * @return 行数
     */
    public int getHeight() {
        return grid.size();
    }

    /**
     * 获取地图总列数。
     *
     * @return 列数
     */
    public int getWidth() {
        return grid.isEmpty() ? 0 : grid.get(0).size();
    }
}
