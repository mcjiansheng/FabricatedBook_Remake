package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Node — 地图节点
 * <p>
 * 代表地图上的一个节点，包含类型、坐标、连接关系和访问状态。
 * 节点通过连接关系形成路径网络，玩家只能在相连的节点间移动。
 * <p>
 * 引用方：MapGraph（持有节点集合）、MapScreen（渲染）、
 *         CombatEngine（进入战斗节点时触发）
 */
public class Node {

    /** 节点类型 */
    private NodeType type;

    /** 节点所在行 */
    private int row;

    /** 节点所在列 */
    private int col;

    /** 节点的屏幕 X 坐标（由地图布局算法计算） */
    private float x;

    /** 节点的屏幕 Y 坐标（由地图布局算法计算） */
    private float y;

    /** 从该节点出发可到达的节点列表（前向连接） */
    private List<Node> connections;

    /** 指向该节点的节点列表（后向连接） */
    private List<Node> incoming;

    /** 是否已被玩家访问过 */
    private boolean visited;

    /** 是否对玩家可见（地图迷雾机制） */
    private boolean visible;

    /** 节点是否可用（已到达的行才可用） */
    private boolean enabled;

    /**
     * 构造地图节点。
     *
     * @param type 节点类型
     * @param row  行号
     * @param col  列号
     */
    public Node(NodeType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.x = 0;
        this.y = 0;
        this.connections = new ArrayList<>();
        this.incoming = new ArrayList<>();
        this.visited = false;
        this.visible = false;
        this.enabled = false;
    }

    // ====== Getter / Setter ======

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public List<Node> getConnections() { return connections; }
    public void setConnections(List<Node> connections) { this.connections = connections; }

    public List<Node> getIncoming() { return incoming; }
    public void setIncoming(List<Node> incoming) { this.incoming = incoming; }

    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // ====== 工具方法 ======

    /**
     * 添加前向连接。
     *
     * @param target 目标节点
     */
    public void addConnection(Node target) {
        if (!connections.contains(target)) {
            connections.add(target);
            target.incoming.add(this);
        }
    }

    /**
     * 添加后向连接。
     *
     * @param source 源节点
     */
    public void addIncoming(Node source) {
        if (!incoming.contains(source)) {
            incoming.add(source);
            source.connections.add(this);
        }
    }

    /**
     * 判断该节点类型是否为战斗节点。
     *
     * @return true 如果是战斗类节点
     */
    public boolean isCombat() {
        return type.isCombat();
    }

    /**
     * 判断该节点是否为事件节点。
     *
     * @return true 如果是事件类节点
     */
    public boolean isEvent() {
        return type.isEvent();
    }

    @Override
    public String toString() {
        return type.getDisplayName() + "(" + row + "," + col + ")";
    }
}
