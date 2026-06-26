package com.fabricatedbook.core.map;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LayerMapGraphTest {
    @Test
    void sameSeedBuildsSameSparseMap() {
        LayerMapConfig config = LayerMapConfig.defaults().get(0);

        String first = signature(new LayerMapGraph(config, 12345L, 0));
        String second = signature(new LayerMapGraph(config, 12345L, 0));
        String different = signature(new LayerMapGraph(config, 54321L, 0));

        assertEquals(first, second);
        assertNotEquals(first, different);
    }

    @Test
    void mistLayerRoutesThroughBossBeforeDecision() {
        LayerMapGraph graph = new LayerMapGraph(LayerMapConfig.defaults().get(3),
                12345L, 3);

        LayerMapNode boss = graph.getNode(5, 0);
        LayerMapNode decision = graph.getNode(6, 0);

        assertNotNull(boss);
        assertNotNull(decision);
        assertEquals(NodeType.BOSS, boss.getType());
        assertEquals(NodeType.DECISION, decision.getType());
        assertTrue(boss.getNext().contains(decision));

        boolean canReachBoss = false;
        for (LayerMapNode node : graph.getColumns()[4]) {
            canReachBoss |= node.getNext().contains(boss);
        }
        assertTrue(canReachBoss);
    }

    @Test
    void availableNodesFollowCurrentNodeConnections() {
        LayerMapGraph graph = new LayerMapGraph(LayerMapConfig.defaults().get(0),
                12345L, 0);

        List<LayerMapNode> availableAtStart = graph.getAvailableNodes();
        assertEquals(1, availableAtStart.size());
        assertEquals(0, availableAtStart.get(0).getCol());

        assertTrue(graph.moveTo(availableAtStart.get(0)));
        assertEquals(graph.getCurrentNode().getNext(), graph.getAvailableNodes());
    }

    private static String signature(LayerMapGraph graph) {
        StringBuilder builder = new StringBuilder();
        for (LayerMapNode[] column : graph.getColumns()) {
            for (LayerMapNode node : column) {
                builder.append(node.getType().name().charAt(0));
                builder.append(node.getNext().size());
            }
            builder.append('/');
        }
        return builder.toString();
    }
}
