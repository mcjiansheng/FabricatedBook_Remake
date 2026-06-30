package com.fabricatedbook.core.encounter;

import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.EntityFactory;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.map.NodeType;
import com.fabricatedbook.data.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolves a map node into the enemy group used by both frontend and backend CLI.
 */
public final class EnemyEncounterResolver {

    private EnemyEncounterResolver() {}

    public static EncounterResult resolve(Player player,
                                          List<DataLoader.EnemyGroup> groups,
                                          int level,
                                          NodeType nodeType,
                                          Random random) {
        Random rng = random == null ? new Random(0) : random;
        if (groups == null) {
            groups = List.of();
        }

        if (nodeType == NodeType.BOSS) {
            EncounterResult forcedBoss = selectForcedBoss(player, groups, level);
            if (forcedBoss != null) {
                return forcedBoss;
            }
        }

        List<DataLoader.EnemyGroup> matched = matchingGroups(groups, nodeType);
        if (matched.isEmpty() && nodeType == NodeType.EMERGENCY) {
            matched = fallbackEmergencyGroups(groups);
        }
        if (!matched.isEmpty()) {
            DataLoader.EnemyGroup selected = selectGroup(matched, nodeType, rng);
            List<Enemy> enemies = toEnemies(selected);
            addBabelTowerEnemy(player, enemies, groups, selected, nodeType, rng);
            if (!enemies.isEmpty()) {
                return new EncounterResult(enemies, selected, false);
            }
        }

        return new EncounterResult(fallbackEnemies(nodeType), null, true);
    }

    public static boolean isHiddenBossRoute(Player player) {
        return player != null
                && player.hasRelic("relic_babel_tower")
                && (player.hasRelic("relic_betrayal")
                || player.hasRelic("relic_hatred"));
    }

    private static EncounterResult selectForcedBoss(Player player,
                                                    List<DataLoader.EnemyGroup> groups,
                                                    int level) {
        String forcedName = null;
        if (level == 3 && player != null && player.hasRelic("relic_avenger")) {
            forcedName = "迷失的守林人";
        } else if (level == 5) {
            forcedName = isHiddenBossRoute(player) ? "幕后黑手" : "魔王";
        }
        if (forcedName == null) {
            return null;
        }
        for (DataLoader.EnemyGroup group : groups) {
            if (!group.isBoss() || !forcedName.equals(group.getName())) {
                continue;
            }
            List<Enemy> enemies = toEnemies(group);
            if (!enemies.isEmpty()) {
                return new EncounterResult(enemies, group, false);
            }
        }
        return null;
    }

    private static List<DataLoader.EnemyGroup> matchingGroups(List<DataLoader.EnemyGroup> groups,
                                                              NodeType nodeType) {
        List<DataLoader.EnemyGroup> matched = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (matchesNodeType(group, nodeType)) {
                matched.add(group);
            }
        }
        return matched;
    }

    private static List<DataLoader.EnemyGroup> fallbackEmergencyGroups(List<DataLoader.EnemyGroup> groups) {
        List<DataLoader.EnemyGroup> matched = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (!group.isBoss() && !isEmergencyGroup(group)) {
                matched.add(group);
            }
        }
        return matched;
    }

    private static DataLoader.EnemyGroup selectGroup(List<DataLoader.EnemyGroup> groups,
                                                     NodeType nodeType,
                                                     Random random) {
        DataLoader.EnemyGroup selected = groups.get(random.nextInt(groups.size()));
        if (nodeType == NodeType.EMERGENCY) {
            for (int attempt = 0; attempt < 3; attempt++) {
                DataLoader.EnemyGroup candidate = groups.get(random.nextInt(groups.size()));
                if (totalHp(candidate) > totalHp(selected)) {
                    selected = candidate;
                }
            }
        }
        return selected;
    }

    private static boolean matchesNodeType(DataLoader.EnemyGroup group, NodeType nodeType) {
        boolean emergency = isEmergencyGroup(group);
        if (nodeType == NodeType.BOSS) {
            return group.isBoss();
        }
        if (nodeType == NodeType.EMERGENCY) {
            return !group.isBoss() && emergency;
        }
        return !group.isBoss() && !emergency;
    }

    private static boolean isEmergencyGroup(DataLoader.EnemyGroup group) {
        String id = group.getId() == null ? "" : group.getId().toLowerCase();
        String name = group.getName() == null ? "" : group.getName();
        return id.contains("emergency") || name.contains("紧急");
    }

    private static int totalHp(DataLoader.EnemyGroup group) {
        if (group.getEnemies() == null) return 0;
        int total = 0;
        for (DataLoader.EnemyData data : group.getEnemies()) {
            total += data.getMaxHp();
        }
        return total;
    }

    private static List<Enemy> toEnemies(DataLoader.EnemyGroup group) {
        List<Enemy> enemies = new ArrayList<>();
        if (group == null || group.getEnemies() == null) {
            return enemies;
        }
        for (DataLoader.EnemyData data : group.getEnemies()) {
            if (data != null) {
                enemies.add(data.toEnemy());
            }
        }
        return enemies;
    }

    private static void addBabelTowerEnemy(Player player,
                                           List<Enemy> enemies,
                                           List<DataLoader.EnemyGroup> groups,
                                           DataLoader.EnemyGroup selected,
                                           NodeType nodeType,
                                           Random random) {
        if (nodeType != NodeType.EMERGENCY || player == null
                || !player.hasRelic("relic_babel_tower")) {
            return;
        }
        List<DataLoader.EnemyData> candidates = new ArrayList<>();
        for (DataLoader.EnemyGroup group : groups) {
            if (group == selected || group.isBoss() || group.getEnemies() == null) {
                continue;
            }
            for (DataLoader.EnemyData data : group.getEnemies()) {
                if (data != null) {
                    candidates.add(data);
                }
            }
        }
        if (!candidates.isEmpty()) {
            enemies.add(candidates.get(random.nextInt(candidates.size())).toEnemy());
        }
    }

    private static List<Enemy> fallbackEnemies(NodeType nodeType) {
        List<Enemy> enemies = new ArrayList<>();
        switch (nodeType) {
            case BOSS -> enemies.add(new Enemy("boss_training", "训练首领", 60,
                    List.of("atk8", "def6", "atk10")));
            case EMERGENCY -> enemies.add(new Enemy("elite_training", "精英训练假人", 42,
                    List.of("atk7", "atk5x2", "def8")));
            default -> enemies.add(EntityFactory.createSimpleEnemy("training_dummy",
                    "训练假人", 32));
        }
        return enemies;
    }

    public static final class EncounterResult {
        private final List<Enemy> enemies;
        private final DataLoader.EnemyGroup group;
        private final boolean fallback;

        private EncounterResult(List<Enemy> enemies,
                                DataLoader.EnemyGroup group,
                                boolean fallback) {
            this.enemies = List.copyOf(enemies);
            this.group = group;
            this.fallback = fallback;
        }

        public List<Enemy> getEnemies() {
            return enemies;
        }

        public DataLoader.EnemyGroup getGroup() {
            return group;
        }

        public boolean isFallback() {
            return fallback;
        }
    }
}
