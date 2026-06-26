package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node in the playable sparse layer map.
 */
public class LayerMapNode {
    private final int col;
    private final int row;
    private final NodeType type;
    private final List<LayerMapNode> next = new ArrayList<>();
    private boolean visited;

    public LayerMapNode(int col, int row, NodeType type) {
        this.col = col;
        this.row = row;
        this.type = type;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }
    public NodeType getType() { return type; }
    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public List<LayerMapNode> getNext() {
        return Collections.unmodifiableList(next);
    }

    public void addNext(LayerMapNode node) {
        if (node != null && !next.contains(node)) {
            next.add(node);
        }
    }

    public int getTypeCode() {
        switch (type) {
            case FIGHT: return 1;
            case EMERGENCY: return 2;
            case BOSS: return 3;
            case UNEXPECTED: return 4;
            case REWARD: return 5;
            case SHOP: return 6;
            case DECISION: return 8;
            case SAFEHOUSE: return 9;
            default: return 1;
        }
    }

    @Override
    public String toString() {
        return type.getDisplayName() + "(" + col + "," + row + ")";
    }
}
