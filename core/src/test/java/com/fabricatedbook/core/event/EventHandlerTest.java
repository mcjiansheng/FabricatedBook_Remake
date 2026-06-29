package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.relic.EventBus;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.data.DataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void firstDecisionCanEndRunImmediately() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult result = handler.executeEvent("命运抉择1", 1);

        assertEquals("ENDING_INTERRUPTED", result.outcome);
        assertNull(result.relicId);
    }

    @Test
    void secondDecisionHidesBabelTowerWithoutEncounterRelic() {
        EventHandler handler = new EventHandler(new Random(1));
        Player player = player();

        List<EventHandler.EventOption> options = handler.getOptions("命运抉择2", player);
        EventHandler.EventResult result = handler.executeEvent("命运抉择2", 2, player);

        assertEquals(2, options.size());
        assertEquals("我要权力", options.get(0).label);
        assertEquals("我要财富", options.get(1).label);
        assertNull(result.relicId);
    }

    @Test
    void secondDecisionAllowsBabelTowerAfterEncounterRelic() {
        EventHandler handler = new EventHandler(new Random(1));
        Player player = player();
        player.addRelic(relic("relic_betrayal", "背叛"));

        List<EventHandler.EventOption> options = handler.getOptions("命运抉择2", player);
        EventHandler.EventResult result = handler.executeEvent("命运抉择2", 2, player);

        assertEquals(3, options.size());
        assertEquals("没有你，对我很重要", options.get(2).label);
        assertEquals("relic_babel_tower", result.relicId);
    }

    @Test
    void decisionDisplayDataComesFromJsonButStaysOutOfRandomPool() {
        EventHandler handler = new EventHandler(new Random(1));

        assertFalse(handler.getEventNames().contains("命运抉择1"));
        assertFalse(handler.getEventNames().contains("命运抉择2"));
        assertEquals("迷雾渐起，前途未知，你将何去何从？",
                handler.getEventDescription("命运抉择1"));

        List<EventHandler.EventOption> options = handler.getOptions("命运抉择1");
        assertEquals(2, options.size());
        assertEquals("前进", options.get(0).label);
        assertEquals("突破迷雾，进入森林", options.get(0).description);
    }

    @Test
    void ordinaryEventTextComesFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        assertTrue(handler.getEventNames().contains("相遇"));
        assertEquals("你偶遇了一个衣衫褴褛的人，他请求与你同行。",
                handler.getEventDescription("相遇"));

        List<EventHandler.EventOption> options = handler.getOptions("相遇");
        assertEquals(3, options.size());
        assertEquals("✅ 同意", options.get(0).label);
        assertEquals("获得藏品「背叛」", options.get(0).description);
    }

    @Test
    void fixedOrdinaryEventResultComesFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult result = handler.executeEvent("相遇", 0);

        assertEquals("relic_betrayal", result.relicId);
        assertEquals(0, result.goldChange);
        assertTrue(result.description.contains("获得藏品「背叛」"));
    }

    @Test
    void fixedEventResolverKeepsOutcomeFieldsFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult result = handler.executeEvent("翅膀雕像", 0);

        assertEquals(-7, result.hpChange);
        assertFalse(result.fullHeal);
        assertEquals(0, result.goldChange);
        assertNull(result.relicId);
        assertNull(result.outcome);
        assertTrue(result.description.contains("失去 7 点生命值"));
    }

    @Test
    void fullHealEventResultIsExplicitInJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult result = handler.executeEvent("人生意义", 0);

        assertTrue(result.fullHeal);
        assertEquals(0, result.hpChange);
        assertTrue(result.description.contains("生命值回满"));
    }

    @Test
    void placeholderRelicEventResultsComeFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult poetry = handler.executeEvent("好诗歪诗", 1);
        EventHandler.EventResult cursePoetry = handler.executeEvent("好诗歪诗", 2);
        EventHandler.EventResult huntEscape = handler.executeEvent("追猎", 1);

        assertEquals("relic_random_leq3", poetry.relicId);
        assertTrue(poetry.description.contains("获得一个藏品"));
        assertEquals("relic_curse_random", cursePoetry.relicId);
        assertEquals("relic_curse_random", huntEscape.relicId);
        assertEquals(20, huntEscape.hpChange);
    }

    @Test
    void placeholderRelicRewardsResolveToConcreteRelics() {
        Player player = player();

        Relic lowValue = EventRewardResolver.resolveRelic("relic_random_leq3",
                player, new Random(1));
        Relic cursed = EventRewardResolver.resolveRelic("relic_curse_random",
                player, new Random(1));

        assertTrue(lowValue != null);
        assertTrue(lowValue.getRarity() != Relic.Rarity.CURSED);
        assertTrue(lowValue.getRarity() != Relic.Rarity.SPECIAL);
        assertTrue(lowValue.getRarity().getValue() <= 3);
        assertTrue(cursed != null);
        assertEquals(Relic.Rarity.CURSED, cursed.getRarity());
    }

    @Test
    void fiveCardEventRewardAddsCardsInsteadOfPlaceholderRelic() {
        Player player = player();
        EventHandler.EventResult result = new EventHandler.EventResult(
                "获得 5 张牌", 0, 0, "relic_five_cards");

        EventRewardResolver.EventReward reward =
                EventRewardResolver.applyRewards(result, player, new Random(1));

        assertEquals(5, reward.getCards().size());
        assertEquals(5, player.getDrawPile().size());
        assertFalse(player.hasRelic("relic_five_cards"));
        for (Card card : player.getDrawPile()) {
            assertTrue(card.getProfession().equals("warrior"));
            assertTrue(card.getRarity() != Card.Rarity.BASIC);
            assertFalse(card.isUnplayable());
        }
    }

    @Test
    void randomEventResultCanComeFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult statue = handler.executeEvent("翅膀雕像", 1);
        EventHandler.EventResult slime = handler.executeEvent("黏液世界", 1);
        EventHandler.EventResult burger = handler.executeEvent("村庄", 2);

        assertTrue(statue.goldChange >= 50 && statue.goldChange <= 80);
        assertTrue(statue.description.contains("金币"));
        assertTrue(slime.goldChange >= -75 && slime.goldChange <= -35);
        assertTrue(slime.description.contains("失去 " + Math.abs(slime.goldChange)));
        assertTrue(burger.hpChange >= 15 && burger.hpChange <= 30);
        assertTrue(burger.description.contains("回复 " + burger.hpChange));
    }

    @Test
    void weightedRandomEventResultCanComeFromJsonData() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult freeInvestment = handler.executeEvent("投资", 0);
        EventHandler.EventResult mediumInvestment = handler.executeEvent("投资", 1);
        EventHandler.EventResult highInvestment = handler.executeEvent("投资", 2);

        assertTrue(List.of(0, 10).contains(freeInvestment.goldChange));
        assertTrue(freeInvestment.description.contains("投资 0 金币"));
        assertTrue(List.of(-50, 0, 100).contains(mediumInvestment.goldChange));
        assertTrue(mediumInvestment.description.contains("投资 50 金币"));
        assertTrue(List.of(-100, 0, 150, 200, 1000).contains(highInvestment.goldChange));
        assertTrue(highInvestment.description.contains("投资 100 金币"));
    }

    @Test
    void fixedJsonEventRelicsCanBeCreated() {
        Player owner = player();

        for (DataLoader.EventData event : new DataLoader().loadEvents()) {
            for (DataLoader.EventOptionData option : event.getOptions()) {
                if (!option.hasExecutableResult() || option.getRelicId() == null
                        || option.getRelicId().isBlank()) {
                    continue;
                }
                assertTrue(RelicFactory.createById(option.getRelicId(), owner) != null,
                        event.getName() + " references unknown relic " + option.getRelicId());
            }
        }
    }

    @Test
    void nonFixedJsonEventOptionsDeclareJavaExecutor() {
        for (DataLoader.EventData event : new DataLoader().loadEvents()) {
            for (DataLoader.EventOptionData option : event.getOptions()) {
                if (option.hasExecutableResult()) {
                    continue;
                }
                assertTrue(option.usesJavaExecutor(),
                        event.getName() + " option needs executor marker: " + option.getText());
            }
        }
    }

    @Test
    void playerPotionLimitIsConfigurable() {
        Player player = player();
        player.setMaxPotionSlots(5);

        for (int i = 0; i < 5; i++) {
            assertTrue(player.addPotion(new Potion("potion_" + i, "药水" + i,
                    "测试药水", List.of())));
        }

        assertEquals(5, player.getPotions().size());
        assertFalse(player.canAddPotion());
        assertFalse(player.addPotion(new Potion("overflow", "额外药水",
                "测试药水", List.of())));
    }

    private Player player() {
        return new Player("p1", "测试玩家", Profession.WARRIOR);
    }

    private Relic relic(String id, String name) {
        return new Relic() {
            @Override public String getName() { return name; }
            @Override public String getDescription() { return ""; }
            @Override public Rarity getRarity() { return Rarity.SPECIAL; }
            @Override public void subscribe(EventBus bus) {}
            @Override public void unsubscribe(EventBus bus) {}
            @Override public String getId() { return id; }
        };
    }
}
