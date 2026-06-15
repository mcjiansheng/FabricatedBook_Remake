package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.buff.Fragile;
import com.fabricatedbook.core.buff.Poison;
import com.fabricatedbook.core.buff.Weak;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatPreviewCalculatorTest {

    @Test
    void cardPreviewUsesTargetBuffsForSingleTargetDamage() {
        Player player = player();
        Enemy enemy = enemy("e1");
        enemy.addBuff(new Fragile(2));
        Card card = attackCard("strike", Card.TargetType.SINGLE_ENEMY, "damage:8");

        CardPreview preview = CombatPreviewCalculator.previewCard(card, player,
                List.of(enemy), enemy, null);

        assertEquals("造成 10 点伤害", preview.getDescription());
    }

    @Test
    void cardPreviewForAllEnemiesOnlyUsesBuffsSharedByAllTargets() {
        Player player = player();
        Enemy fragileEnemy = enemy("e1");
        fragileEnemy.addBuff(new Fragile(2));
        Enemy plainEnemy = enemy("e2");
        Card card = attackCard("sweep", Card.TargetType.ALL_ENEMIES, "damage_all:5");

        CardPreview mixedPreview = CombatPreviewCalculator.previewCard(card, player,
                List.of(fragileEnemy, plainEnemy), null, null);
        assertEquals("造成 5 点伤害", mixedPreview.getDescription());

        plainEnemy.addBuff(new Fragile(1));
        CardPreview sharedPreview = CombatPreviewCalculator.previewCard(card, player,
                List.of(fragileEnemy, plainEnemy), null, null);
        assertEquals("造成 7 点伤害", sharedPreview.getDescription());
    }

    @Test
    void enemyIntentPreviewUsesCurrentBuffsAndPlayerIncomingRelics() {
        Player player = player();
        Enemy enemy = enemy("e1");
        enemy.addBuff(new Weak(1));
        player.addBuff(new Fragile(1));

        EnemyIntentPreview preview = CombatPreviewCalculator.previewEnemyIntent(enemy,
                player, List.of(enemy), null, "atk10");

        assertEquals("9", preview.getDetail());
    }

    @Test
    void poisonDirectlyLosesHpWithoutConsumingBlockOrDoubleCounting() {
        Player player = player();
        player.setHp(20);
        player.setBlock(6);
        Poison poison = new Poison(3);
        player.addBuff(poison);

        poison.onTurnStart(player);

        assertEquals(17, player.getHp());
        assertEquals(6, player.getBlock());
        assertEquals(2, poison.getStack());
    }

    @Test
    void blockedEnemyAttackDoesNotDealHpDamageBeforePoisonTicks() {
        Player player = player();
        player.setHp(20);
        player.setBlock(6);
        player.addBuff(new Fragile(2));
        Enemy enemy = enemy("thief");

        DamageAction attack = new DamageAction(enemy, List.of(player), 3);
        attack.execute();

        assertEquals(20, player.getHp());
        assertEquals(2, player.getBlock());
    }

    @Test
    void poisonAttackIntentShowsDebuffPreview() {
        Player player = player();
        Enemy thief = new Enemy("thief", "盗贼", 35, List.of("atk_poison_3"));

        EnemyIntentPreview preview = CombatPreviewCalculator.previewEnemyIntent(thief,
                player, List.of(thief), null, "atk_poison_3");

        assertEquals("2", preview.getDetail());
        assertEquals("中毒3", preview.getDebuffDetail());
    }

    @Test
    void cardPreviewUsesStableRelicsAndSkipsProbabilisticRelics() {
        Player player = player();
        Enemy enemy = enemy("e1");
        Relic stable = RelicFactory.createById("relic_a_combo", player);
        Relic random = RelicFactory.createById("relic_target", player);
        player.addRelic(stable);
        player.addRelic(random);
        RelicManager relicManager = new RelicManager(player);
        Card card = attackCard("strike", Card.TargetType.SINGLE_ENEMY, "damage:10");

        CardPreview preview = CombatPreviewCalculator.previewCard(card, player,
                List.of(enemy), enemy, relicManager);

        assertEquals("造成 11 点伤害", preview.getDescription());
    }

    @Test
    void cardPreviewSimulatesEarlierBuffsInTheSameCardWithoutMutatingPlayer() {
        Player player = player();
        Card card = new Card("block", "不屈", 1, "测试描述", Card.CardType.DEFENSE,
                Card.Rarity.COMMON, 1, Card.TargetType.SELF, 1,
                List.of("buff:self:BlockIncrease:3", "block:10"),
                false, "warrior");

        CardPreview preview = CombatPreviewCalculator.previewCard(card, player,
                List.of(), null, null);

        assertEquals("获得 15 点格挡", preview.getDescription());
        assertEquals(0, player.getBuffs().size());
    }

    @Test
    void combatStartResetClearsTemporaryStateAndRebuildsDrawPile() {
        Player player = player();
        player.setBlock(12);
        player.setEnergy(2);
        player.addBuff(new Weak(1));
        player.getDrawPile().add(attackCard("draw", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));
        player.getHand().add(attackCard("hand", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));
        player.getDiscardPile().add(attackCard("discard", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));

        player.resetForCombatStart();

        assertEquals(0, player.getBlock());
        assertEquals(0, player.getEnergy());
        assertTrue(player.getBuffs().isEmpty());
        assertTrue(player.getHand().isEmpty());
        assertTrue(player.getDiscardPile().isEmpty());
        assertEquals(3, player.getDrawPile().size());
    }

    private static Player player() {
        return new Player("p1", "战士", Profession.WARRIOR);
    }

    private static Enemy enemy(String id) {
        return new Enemy(id, "测试敌人", 40, List.of("atk10"));
    }

    private static Card attackCard(String id, Card.TargetType targetType, String... effects) {
        return new Card(id, "测试卡", 1, "测试描述", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, targetType, 1, List.of(effects),
                false, "warrior");
    }
}
