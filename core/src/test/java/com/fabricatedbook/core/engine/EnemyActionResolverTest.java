package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.data.DataLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnemyActionResolverTest {

    @Test
    void allConfiguredMonsterActionsResolveWithoutFallback() {
        DataLoader loader = new DataLoader();
        Player player = new Player("enemy-action-test", "敌人行动测试",
                Profession.WARRIOR);

        for (int level = 1; level <= 5; level++) {
            for (DataLoader.EnemyGroup group : loader.loadMonsters(level)) {
                List<Enemy> enemies = new ArrayList<>();
                for (DataLoader.EnemyData enemyData : group.getEnemies()) {
                    enemies.add(enemyData.toEnemy());
                }
                for (Enemy enemy : enemies) {
                    for (String actionId : enemy.getActionScript()) {
                        List<CombatAction> actions = EnemyActionResolver.resolve(
                                enemy, actionId, player, enemies, new Random(1));
                        assertNotNull(actions, group.getId() + "/" + enemy.getId()
                                + " action should be handled: " + actionId);
                    }
                }
            }
        }
    }
}
