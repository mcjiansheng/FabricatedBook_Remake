package com.fabricatedbook.data;

import com.fabricatedbook.core.map.LayerMapConfig;
import com.fabricatedbook.core.map.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataLoaderTest {
    @Test
    void loadsSparseLayerMapConfigsFromJson() {
        List<LayerMapConfig> configs = new DataLoader().loadLayerMapConfigs();

        assertEquals(5, configs.size());
        assertEquals("荒野", configs.get(0).getLevelName());
        assertEquals(4, configs.get(0).getLength());
        assertEquals(3, configs.get(0).getWidth());

        LayerMapConfig mist = configs.get(3);
        assertEquals(NodeType.EMERGENCY, mist.getStartType());
        assertEquals(NodeType.DECISION, mist.getEndType());
        assertEquals(5, mist.getSpecialBossColumn());
    }
}
