package com.fabricatedbook.core.engine;

import com.fabricatedbook.core.buff.Fragile;
import com.fabricatedbook.core.buff.ArmorBuff;
import com.fabricatedbook.core.buff.ExtraEnergyBuff;
import com.fabricatedbook.core.buff.UndeadBuff;
import com.fabricatedbook.core.buff.Withering;
import com.fabricatedbook.core.buff.Poison;
import com.fabricatedbook.core.buff.Weak;
import com.fabricatedbook.core.action.ApplyBuffAction;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.action.DoublePoisonAction;
import com.fabricatedbook.core.action.TriggerWitheringAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.data.DataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void playerPoisonDamageCanBeBlocked() {
        Player player = player();
        player.setHp(20);
        player.setBlock(6);
        Poison poison = new Poison(3);
        player.addBuff(poison);

        poison.tick(player, true);

        assertEquals(20, player.getHp());
        assertEquals(3, player.getBlock());
        assertEquals(2, poison.getStack());
    }

    @Test
    void enemyPoisonDamagePiercesBlock() {
        Enemy enemy = enemy("e1");
        enemy.setBlock(6);
        Poison poison = new Poison(3);
        enemy.addBuff(poison);

        poison.tick(enemy, false);

        assertEquals(37, enemy.getHp());
        assertEquals(6, enemy.getBlock());
        assertEquals(2, poison.getStack());
    }

    @Test
    void enemyPoisonTicksBeforeEnemyActionAndCanPreventIt() {
        Player player = player();
        Enemy enemy = new Enemy("e1", "测试敌人", 3, List.of("atk10"));
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));
        enemy.addBuff(new Poison(3));
        enemy.setBlock(20);

        engine.endRound();

        assertEquals(80, player.getHp());
        assertTrue(engine.isVictory());
    }

    @Test
    void enemyAppliedPoisonDoesNotTickUntilNextPlayerTurnEnds() {
        Player player = player();
        Enemy enemy = new Enemy("thief", "盗贼", 35, List.of("atk_poison_3"));
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));

        engine.endRound();

        assertEquals(78, player.getHp());
        assertEquals(3, poisonStack(player));
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
    void cardPreviewOnlyReplacesDamageNumberAndKeepsConditionalText() {
        Player player = player();
        Enemy healthy = enemy("healthy");
        healthy.setHp(30);
        Enemy wounded = enemy("wounded");
        wounded.setHp(29);
        Card dragonFang = new Card("dragon", "龙牙", 1,
                "造成 9 点伤害，敌方生命值 <30 则伤害 +5",
                Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:9", "bonus_low_hp:30:5"), false, "warrior");

        CardPreview healthyPreview = CombatPreviewCalculator.previewCard(dragonFang, player,
                List.of(healthy), healthy, null);
        CardPreview woundedPreview = CombatPreviewCalculator.previewCard(dragonFang, player,
                List.of(wounded), wounded, null);

        assertEquals("造成 9 点伤害，敌方生命值 <30 则伤害 +5",
                healthyPreview.getDescription());
        assertEquals("造成 14 点伤害，敌方生命值 <30 则伤害 +5",
                woundedPreview.getDescription());
    }

    @Test
    void cardPreviewAppliesTargetVulnerabilityAfterConditionalDamageBonus() {
        Player player = player();
        Enemy wounded = enemy("wounded");
        wounded.setHp(29);
        wounded.addBuff(new Fragile(1));
        Card dragonFang = new Card("dragon", "龙牙", 1,
                "造成 9 点伤害，敌方生命值 <30 则伤害 +5",
                Card.CardType.ATTACK, Card.Rarity.COMMON, 1,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:9", "bonus_low_hp:30:5"), false, "warrior");

        CardPreview preview = CombatPreviewCalculator.previewCard(dragonFang, player,
                List.of(wounded), wounded, null);

        assertEquals("造成 18 点伤害，敌方生命值 <30 则伤害 +5",
                preview.getDescription());
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
        player.getExhaustPile().add(attackCard("exhaust", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));

        player.resetForCombatStart();

        assertEquals(0, player.getBlock());
        assertEquals(0, player.getEnergy());
        assertTrue(player.getBuffs().isEmpty());
        assertTrue(player.getHand().isEmpty());
        assertTrue(player.getDiscardPile().isEmpty());
        assertTrue(player.getExhaustPile().isEmpty());
        assertEquals(4, player.getDrawPile().size());
    }

    @Test
    void shuffleDiscardToDrawDoesNotReturnExhaustedCards() {
        Player player = player();
        player.getDiscardPile().add(attackCard("discard", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));
        player.getExhaustPile().add(attackCard("exhaust", Card.TargetType.SINGLE_ENEMY,
                "damage:1"));

        player.shuffleDiscardToDraw();

        assertEquals(1, player.getDrawPile().size());
        assertTrue(player.getDiscardPile().isEmpty());
        assertEquals(1, player.getExhaustPile().size());
    }

    @Test
    void endOfTurnRetainsRetainCardsAndExhaustsEtherealCards() {
        Player player = player();
        Card retain = attackCard("retain", Card.TargetType.SINGLE_ENEMY, "damage:1");
        retain.setRetain(true);
        Card ethereal = attackCard("ethereal", Card.TargetType.SINGLE_ENEMY, "damage:1");
        ethereal.setEthereal(true);
        Card normal = attackCard("normal", Card.TargetType.SINGLE_ENEMY, "damage:1");
        player.getHand().add(retain);
        player.getHand().add(ethereal);
        player.getHand().add(normal);

        player.resolveEndOfTurnHand();

        assertEquals(List.of(retain), player.getHand());
        assertEquals(List.of(normal), player.getDiscardPile());
        assertEquals(List.of(ethereal), player.getExhaustPile());
    }

    @Test
    void xCostCardSpendsAllCurrentEnergyAndRepeatsDamage() {
        Player player = player();
        Card eradicate = new Card("x", "根除", -1, "测试描述",
                Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SINGLE_ENEMY, 1, List.of("damage_x:11"),
                false, true, false, "warrior");
        player.getDrawPile().add(eradicate);
        Enemy enemy = enemy("e1");
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));

        engine.playCard(eradicate, enemy);

        assertEquals(0, player.getEnergy());
        assertEquals(7, enemy.getHp());
        assertEquals(List.of(eradicate), player.getDiscardPile());
    }

    @Test
    void xCostCardPreviewUsesCurrentEnergyAsRepeatCount() {
        Player player = player();
        player.setEnergy(2);
        Enemy enemy = enemy("e1");
        Card card = new Card("x", "根除", -1, "测试描述",
                Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SINGLE_ENEMY, 1, List.of("damage_x:11"),
                false, true, false, "warrior");

        CardPreview preview = CombatPreviewCalculator.previewCard(card, player,
                List.of(enemy), enemy, null);

        assertEquals("造成 2 × 11 点伤害", preview.getDescription());
    }

    @Test
    void cardEffectsResolveInTextOrder() {
        Player player = player();
        Card painfulBlow = new Card("pain", "痛击", 2,
                "造成 8 点伤害，提供 2 点脆弱", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:8", "debuff:Fragile:2"), false, "warrior");
        player.getDrawPile().add(painfulBlow);
        Enemy enemy = enemy("e1");
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));

        assertTrue(engine.playCard(painfulBlow, enemy));

        assertEquals(32, enemy.getHp());
        assertTrue(enemy.hasBuff("Fragile"));
    }

    @Test
    void repeatedDamagePreviewUsesCountTimesDamageFormat() {
        Player player = player();
        Enemy enemy = enemy("e1");
        Card poisoned = new Card("poisoned", "淬毒", 1,
                "造成 5×2 点伤害，每次 50% 概率附加中毒",
                Card.CardType.ATTACK, Card.Rarity.UNCOMMON, 2,
                Card.TargetType.SINGLE_ENEMY, 1,
                List.of("damage:2:5"), false, "warrior");

        CardPreview preview = CombatPreviewCalculator.previewCard(poisoned, player,
                List.of(enemy), enemy, null);

        assertEquals("造成 5 × 2 点伤害，每次 50% 概率附加中毒",
                preview.getDescription());
    }

    @Test
    void abilityCardsLeaveCombatPilesAfterPlay() {
        Player player = player();
        Card armor = new Card("armor", "装甲", 2,
                "格挡值不再在回合结束时消失", Card.CardType.ABILITY,
                Card.Rarity.EPIC, 3, Card.TargetType.SELF, 1,
                List.of("buff:self:armor"), false, "warrior");
        player.getDrawPile().add(armor);
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy("e1")));

        assertTrue(engine.playCard(armor, null));

        assertFalse(player.getHand().contains(armor));
        assertFalse(player.getDiscardPile().contains(armor));
        assertFalse(player.getExhaustPile().contains(armor));
        assertTrue(player.hasBuff("ArmorBuff"));
    }

    @Test
    void unplayableBleedingDamagesAtEndOfTurnAndExhausts() {
        Player player = player();
        player.setHp(20);
        Card bleeding = new Card("bleed", "流血", 0,
                "无法打出，虚无。回合结束时若在手中则受到 1 点伤害",
                Card.CardType.STATUS, Card.Rarity.BASIC, 0,
                Card.TargetType.SELF, 1, List.of("end_turn_damage:1"),
                false, false, true, true, "warrior");
        player.getHand().add(bleeding);

        player.resolveEndOfTurnHand();

        assertEquals(19, player.getHp());
        assertEquals(List.of(bleeding), player.getExhaustPile());
    }

    @Test
    void unplayableCardsCannotBePlayed() {
        Player player = player();
        Card bleeding = new Card("bleed", "流血", 0, "无法打出",
                Card.CardType.STATUS, Card.Rarity.BASIC, 0,
                Card.TargetType.SELF, 1, List.of(), false, false, true,
                true, "warrior");
        player.getDrawPile().add(bleeding);
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy("e1")));

        assertFalse(engine.playCard(bleeding, null));

        assertTrue(player.getHand().contains(bleeding));
    }

    @Test
    void bloodDanceAddsBleedingToDiscardPile() {
        Player player = player();
        Card bloodDance = CardFactory.createFromTemplate(CardPool.findById("war_blood_dance"));
        player.getDrawPile().add(bloodDance);
        Enemy enemy = enemy("e1");
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));

        assertTrue(engine.playCard(bloodDance, null));

        assertEquals(25, enemy.getHp());
        assertTrue(player.getDiscardPile().stream()
                .anyMatch(card -> "war_bleeding".equals(card.getId())));
    }

    @Test
    void fateSealedRepeatsForEachAttackingIntentEnemy() {
        Player player = player();
        Card fateSealed = CardFactory.createFromTemplate(CardPool.findById("war_fate_sealed"));
        player.getDrawPile().add(fateSealed);
        Enemy attackingA = new Enemy("a", "攻击者A", 40, List.of("atk10"));
        Enemy defending = new Enemy("d", "防御者", 40, List.of("def5"));
        Enemy attackingB = new Enemy("b", "攻击者B", 40, List.of("atk10"));
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(attackingA, defending, attackingB));

        assertTrue(engine.playCard(fateSealed, null));

        assertEquals(20, attackingA.getHp());
        assertEquals(40, defending.getHp());
        assertEquals(20, attackingB.getHp());
    }

    @Test
    void warriorStarterDeckIncludesPainfulBlowAsBasicCard() {
        Player player = player();
        CombatEngine engine = new CombatEngine();

        engine.initBattle(player, List.of(enemy("e1")));

        long painfulBlows = player.getHand().stream()
                .filter(card -> "war_painful_blow".equals(card.getId()))
                .count()
                + player.getDrawPile().stream()
                .filter(card -> "war_painful_blow".equals(card.getId()))
                .count();
        assertEquals(1, painfulBlows);
        assertEquals(Card.Rarity.BASIC, CardPool.findById("war_painful_blow").getRarity());
        assertEquals(0, CardPool.findById("war_painful_blow").getValue());
    }

    @Test
    void basicCardsAreExcludedFromNaturalCardPools() {
        List<Card> pool = CardPool.getObtainableCardsByProfession("warrior");

        assertTrue(pool.stream().noneMatch(card -> card.getRarity() == Card.Rarity.BASIC));
        assertTrue(pool.stream().noneMatch(card -> "war_painful_blow".equals(card.getId())));
    }

    @Test
    void cardUpgradeAppliesTemplateRulesAndNameSuffix() {
        Player player = player();
        Enemy enemy = enemy("e1");
        Card attack = CardFactory.createFromTemplate(CardPool.findById("war_atk1"));

        assertTrue(attack.canUpgrade());
        assertTrue(attack.upgrade());

        assertEquals("攻击+", attack.getName());
        assertEquals("攻击", attack.getBaseName());
        assertFalse(attack.canUpgrade());
        assertEquals(List.of("damage:9"), attack.getEffects());
        CardPreview preview = CombatPreviewCalculator.previewCard(attack, player,
                List.of(enemy), enemy, null);
        assertEquals("造成 9 点伤害", preview.getDescription());
    }

    @Test
    void upgradedCardsSurviveRunSnapshotRestore() {
        Player player = player();
        Card sweep = CardFactory.createFromTemplate(CardPool.findById("war_sweep"));
        assertTrue(sweep.upgrade());
        player.getDrawPile().add(sweep);

        Player restored = GameRunState.PlayerSnapshot.from(player).toPlayer();

        assertEquals(1, restored.getDrawPile().size());
        Card restoredCard = restored.getDrawPile().get(0);
        assertEquals("横扫+", restoredCard.getName());
        assertTrue(restoredCard.isUpgraded());
        assertEquals(List.of("damage_all:7"), restoredCard.getEffects());
    }

    @Test
    void statusCardsCannotUpgrade() {
        Card bleeding = CardFactory.createFromTemplate(CardPool.findById("war_bleeding"));

        assertFalse(bleeding.canUpgrade());
        assertFalse(bleeding.upgrade());
        assertEquals("流血", bleeding.getName());
    }

    @Test
    void upgradedPlagueTriplesPoisonAfterApplyingPoison() {
        Enemy enemy = enemy("e1");
        enemy.addBuff(new Poison(3));
        Card plague = CardFactory.createFromTemplate(CardPool.findById("war_plague"));
        assertTrue(plague.upgrade());
        Player player = player();
        player.getDrawPile().add(plague);
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));

        assertTrue(engine.playCard(plague, null));

        assertEquals(27, enemy.getBuffs().stream()
                .filter(buff -> "Poison".equals(buff.getBuffName()))
                .findFirst()
                .orElseThrow()
                .getStack());
    }

    @Test
    void armorBuffPreservesBlockAcrossRoundStart() {
        Player player = player();
        Enemy enemy = new Enemy("idle", "测试敌人", 40, List.of("idle"));
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));
        player.addBuff(new ArmorBuff(1));
        player.setBlock(12);

        engine.endRound();

        assertEquals(12, player.getBlock());
    }

    @Test
    void undeadKeepsEntityAliveAtZeroHpUntilTickExpires() {
        Player player = player();
        UndeadBuff undead = new UndeadBuff(1);
        player.addBuff(undead);

        player.setHp(0);

        assertTrue(player.isAlive());

        undead.tick(player);

        assertEquals(0, player.getHp());
        assertTrue(!player.isAlive());
    }

    @Test
    void extraEnergyBuffAddsEnergyForLimitedTurns() {
        Player player = player();
        Enemy enemy = new Enemy("idle", "测试敌人", 40, List.of("idle"));
        CombatEngine engine = new CombatEngine();
        engine.initBattle(player, List.of(enemy));
        player.addBuff(new ExtraEnergyBuff(3, 2));

        engine.endRound();

        assertEquals(5, player.getEnergy());
        assertEquals(2, player.getBuffs().stream()
                .filter(buff -> "ExtraEnergyBuff".equals(buff.getBuffName()))
                .findFirst()
                .orElseThrow()
                .getStack());
    }

    @Test
    void queuedWitheringTriggerUsesBuffAppliedEarlierInSameCard() {
        Enemy enemy = enemy("e1");
        enemy.setBlock(10);
        new ApplyBuffAction(enemy, "Withering", 4).execute();

        new TriggerWitheringAction(enemy, 1).execute();

        assertEquals(36, enemy.getHp());
        assertEquals(10, enemy.getBlock());
        Withering withering = (Withering) enemy.getBuffs().stream()
                .filter(buff -> buff instanceof Withering)
                .findFirst()
                .orElseThrow();
        assertEquals(0, withering.getStack());
    }

    @Test
    void queuedDoublePoisonIncludesPoisonAppliedEarlierInSameCard() {
        Enemy enemy = enemy("e1");
        enemy.addBuff(new Poison(3));
        new ApplyBuffAction(enemy, "Poison", 6).execute();

        new DoublePoisonAction(List.of(enemy)).execute();

        assertEquals(18, enemy.getBuffs().stream()
                .filter(buff -> "Poison".equals(buff.getBuffName()))
                .findFirst()
                .orElseThrow()
                .getStack());
    }

    @Test
    void dataLoaderPreservesEnemyPassiveFromJson() {
        DataLoader.EnemyData data = new DataLoader().loadMonsters(2).stream()
                .flatMap(group -> group.getEnemies().stream())
                .filter(enemyData -> "treeman".equals(enemyData.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals("def_bark_3", data.toEnemy().getPassive());
    }

    @Test
    void enemyTurnStartPassiveAppliesDuringCombatStartRound() {
        Player player = player();
        Enemy treeman = new Enemy("treeman", "树人", 40, List.of("idle"),
                "def_bark_3");
        CombatEngine engine = new CombatEngine();

        engine.initBattle(player, List.of(treeman));

        assertEquals(3, treeman.getBlock());
    }

    @Test
    void potionDamageAndBlockUseCombatCalculators() {
        Player player = player();
        player.addBuff(new com.fabricatedbook.core.buff.Strength(1));
        player.addBuff(new com.fabricatedbook.core.buff.BlockIncrease(1));
        Enemy enemy = enemy("e1");
        enemy.addBuff(new Fragile(1));
        Potion attackPotion = new Potion("atk", "攻击药水", "",
                List.of("damage_all:10"));
        Potion shieldPotion = new Potion("block", "护盾药水", "",
                List.of("block:10"));

        attackPotion.use(player, List.of(enemy), null);
        shieldPotion.use(player, List.of(enemy), null);

        assertEquals(23, enemy.getHp());
        assertEquals(15, player.getBlock());
    }

    @Test
    void relicsModifyStatusDamageAndCumulativeCombatWins() {
        Player player = player();
        Relic marionette = RelicFactory.createById("relic_marionette", player);
        Relic witherStorm = RelicFactory.createById("relic_wither_storm", player);
        Relic frostmourne = RelicFactory.createById("relic_frostmourne", player);
        player.addRelic(marionette);
        player.addRelic(witherStorm);
        player.addRelic(frostmourne);
        RelicManager relicManager = new RelicManager(player);
        Enemy enemy = enemy("e1");
        enemy.setStatusDamageModifier(relicManager::modifyStatusDamage);
        enemy.addBuff(new Withering(10));

        new TriggerWitheringAction(enemy, 1).execute();
        relicManager.onCombatVictory();
        int damage = relicManager.modifyDamage(100, player, enemy);

        assertEquals(23, enemy.getHp());
        assertEquals(108, damage);
    }

    private static Player player() {
        return new Player("p1", "战士", Profession.WARRIOR);
    }

    private static Enemy enemy(String id) {
        return new Enemy(id, "测试敌人", 40, List.of("atk10"));
    }

    private static int poisonStack(Player player) {
        return player.getBuffs().stream()
                .filter(buff -> "Poison".equals(buff.getBuffName()))
                .mapToInt(com.fabricatedbook.core.buff.BuffHook::getStack)
                .findFirst()
                .orElse(0);
    }

    private static Card attackCard(String id, Card.TargetType targetType, String... effects) {
        return new Card(id, "测试卡", 1, "测试描述", Card.CardType.ATTACK,
                Card.Rarity.COMMON, 1, targetType, 1, List.of(effects),
                false, "warrior");
    }
}
