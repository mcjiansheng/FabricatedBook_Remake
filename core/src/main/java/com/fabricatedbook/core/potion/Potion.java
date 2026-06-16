package com.fabricatedbook.core.potion;

import com.fabricatedbook.core.action.ApplyBuffAction;
import com.fabricatedbook.core.action.GainBlockAction;
import com.fabricatedbook.core.action.TriggerWitheringAction;
import com.fabricatedbook.core.engine.DamageCalculator;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.RelicManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Potion — 数据驱动药水。
 */
public class Potion {

    private String id;
    private String name;
    private String description;
    private List<String> effects;

    public Potion() {
        this.effects = new ArrayList<>();
    }

    public Potion(String id, String name, String description, List<String> effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
    }

    public Potion copy() {
        return new Potion(id, name, description, effects);
    }

    public boolean use(Player player, List<Enemy> enemies, RelicManager relicManager) {
        if (player == null || effects == null) return false;

        List<AbstractEntity> aliveEnemies = new ArrayList<>();
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    aliveEnemies.add(enemy);
                }
            }
        }

        for (String effect : effects) {
            applyEffect(effect, player, aliveEnemies, relicManager);
        }
        return true;
    }

    private void applyEffect(String effect, Player player,
                             List<AbstractEntity> aliveEnemies,
                             RelicManager relicManager) {
        String[] parts = effect.split(":");
        String type = parts[0];
        switch (type) {
            case "heal" -> {
                int amount = Integer.parseInt(parts[1]);
                if (relicManager != null) {
                    amount = relicManager.modifyHeal(amount);
                }
                player.heal(amount);
            }
            case "damage_all" -> {
                int damage = Integer.parseInt(parts[1]);
                for (AbstractEntity enemy : aliveEnemies) {
                    int finalDamage = DamageCalculator.calculateDamage(damage, player, enemy);
                    if (relicManager != null) {
                        finalDamage = relicManager.modifyDamage(finalDamage, player, enemy);
                    }
                    enemy.takeDamage(finalDamage);
                }
            }
            case "block" -> new GainBlockAction(player, Integer.parseInt(parts[1])).execute();
            case "energy" -> player.gainEnergy(Integer.parseInt(parts[1]));
            case "draw" -> player.drawCards(Integer.parseInt(parts[1]));
            case "buff" -> {
                String buffName = parts[2];
                int stack = Integer.parseInt(parts[3]);
                new ApplyBuffAction(player, buffName, stack).execute();
            }
            case "debuff_all" -> {
                String buffName = parts[1];
                int stack = Integer.parseInt(parts[2]);
                for (AbstractEntity enemy : aliveEnemies) {
                    new ApplyBuffAction(enemy, buffName, stack).execute();
                }
            }
            case "remove_all_enemy_block" -> {
                for (AbstractEntity enemy : aliveEnemies) {
                    enemy.setBlock(0);
                }
            }
            case "trigger_withering_all" -> {
                for (AbstractEntity enemy : aliveEnemies) {
                    triggerWithering(enemy);
                }
            }
            default -> System.out.println("[Potion] 未知效果: " + effect);
        }
    }

    private void triggerWithering(AbstractEntity target) {
        new TriggerWitheringAction(target, 1).execute();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getEffects() { return effects; }
}
