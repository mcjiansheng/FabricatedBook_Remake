package com.fabricatedbook.core.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Column-first sparse map configuration used by the playable run map.
 */
public class LayerMapConfig {
    private final int level;
    private final String levelName;
    private final String effectText;
    private final int length;
    private final int width;
    private final NodeType startType;
    private final NodeType endType;
    private final int[] probabilities;
    private final int specialBossColumn;

    public LayerMapConfig(int level, String levelName, String effectText,
                          int length, int width, NodeType startType,
                          NodeType endType, int[] probabilities,
                          int specialBossColumn) {
        this.level = level;
        this.levelName = levelName;
        this.effectText = effectText;
        this.length = length;
        this.width = width;
        this.startType = startType;
        this.endType = endType;
        this.probabilities = probabilities.clone();
        this.specialBossColumn = specialBossColumn;
    }

    public static List<LayerMapConfig> defaults() {
        List<LayerMapConfig> configs = new ArrayList<>();
        configs.add(new LayerMapConfig(1, "荒野", "当前层效果：无",
                4, 3, NodeType.FIGHT, NodeType.DECISION,
                new int[]{60, 10, 0, 30, 0, 0, 0, 0, 0}, -1));
        configs.add(new LayerMapConfig(2, "森林", "当前层效果：奖励与商店出现",
                5, 3, NodeType.FIGHT, NodeType.SHOP,
                new int[]{40, 20, 0, 10, 10, 10, 0, 0, 10}, -1));
        configs.add(new LayerMapConfig(3, "诡异秘林", "当前层效果：Boss 节点出现",
                6, 4, NodeType.REWARD, NodeType.BOSS,
                new int[]{40, 20, 0, 10, 10, 10, 0, 0, 10}, -1));
        configs.add(new LayerMapConfig(4, "迷雾", "当前层效果：路线更窄，Boss 后出现门扉",
                7, 4, NodeType.EMERGENCY, NodeType.DECISION,
                new int[]{40, 20, 0, 10, 10, 10, 0, 0, 10}, 5));
        configs.add(new LayerMapConfig(5, "高塔", "当前层效果：最终高塔",
                7, 4, NodeType.UNEXPECTED, NodeType.BOSS,
                new int[]{40, 20, 0, 10, 10, 10, 0, 0, 10}, -1));
        return configs;
    }

    public int getLevel() { return level; }
    public String getLevelName() { return levelName; }
    public String getEffectText() { return effectText; }
    public int getLength() { return length; }
    public int getWidth() { return width; }
    public NodeType getStartType() { return startType; }
    public NodeType getEndType() { return endType; }
    public int[] getProbabilities() { return probabilities.clone(); }
    public int getSpecialBossColumn() { return specialBossColumn; }
    public boolean hasSpecialBossColumn() { return specialBossColumn >= 0; }
}
