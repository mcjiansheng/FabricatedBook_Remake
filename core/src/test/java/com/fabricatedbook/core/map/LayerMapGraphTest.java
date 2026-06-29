package com.fabricatedbook.core.map;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    void defaultLayerMapsHaveLegalReachableConnections() {
        long[] seeds = {1L, 12345L, 54321L, 987654321L};
        List<LayerMapConfig> configs = LayerMapConfig.defaults();

        for (int layer = 0; layer < configs.size(); layer++) {
            for (long seed : seeds) {
                LayerMapGraph graph = new LayerMapGraph(configs.get(layer), seed, layer);
                assertLegalReachableGraph(graph, configs.get(layer),
                        "layer=" + layer + ", seed=" + seed);
            }
        }
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
                builder.append('(').append(node.getCol()).append(',')
                        .append(node.getRow()).append(")->");
                for (LayerMapNode next : node.getNext()) {
                    builder.append(next.getCol()).append(',')
                            .append(next.getRow()).append(';');
                }
                builder.append('|');
            }
            builder.append('/');
        }
        return builder.toString();
    }

    private static void assertLegalReachableGraph(LayerMapGraph graph,
                                                  LayerMapConfig config,
                                                  String context) {
        LayerMapNode[][] columns = graph.getColumns();
        assertEquals(config.getLength(), columns.length, context);
        assertEquals(1, columns[0].length, context + " start column");
        assertEquals(1, columns[columns.length - 1].length, context + " end column");

        Set<LayerMapNode> incoming = new HashSet<>();
        for (int col = 0; col < columns.length; col++) {
            assertTrue(columns[col].length > 0, context + " empty column " + col);
            for (LayerMapNode node : columns[col]) {
                assertEquals(col, node.getCol(), context + " node column");
                if (col == columns.length - 1) {
                    assertTrue(node.getNext().isEmpty(),
                            context + " final node should not have outgoing edges");
                } else {
                    assertFalse(node.getNext().isEmpty(),
                            context + " non-final node should have outgoing edges");
                }
                for (LayerMapNode next : node.getNext()) {
                    assertEquals(col + 1, next.getCol(),
                            context + " edge should point to next column");
                    assertSame(columns[next.getCol()][next.getRow()], next,
                            context + " edge target should be graph node instance");
                    incoming.add(next);
                }
            }
        }

        for (int col = 1; col < columns.length; col++) {
            for (LayerMapNode node : columns[col]) {
                assertTrue(incoming.contains(node),
                        context + " node should be reachable: " + node);
            }
        }

        assertEquals(config.getStartType(), columns[0][0].getType(),
                context + " start type");
        assertEquals(config.getEndType(), columns[columns.length - 1][0].getType(),
                context + " end type");
        if (config.hasSpecialBossColumn()) {
            LayerMapNode[] bossColumn = columns[config.getSpecialBossColumn()];
            assertEquals(1, bossColumn.length, context + " special boss column size");
            assertEquals(NodeType.BOSS, bossColumn[0].getType(),
                    context + " special boss type");
            assertTrue(bossColumn[0].getNext().contains(columns[columns.length - 1][0]),
                    context + " special boss should lead to end");
        }
    }
}
