package com.fabricatedbook.core.event;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.core.potion.Potion;
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
    void ordinaryEventWithoutJsonOutcomeFallsBackToJavaHandler() {
        EventHandler handler = new EventHandler(new Random(1));

        EventHandler.EventResult result = handler.executeEvent("翅膀雕像", 1);

        assertTrue(result.goldChange >= 50 && result.goldChange <= 80);
        assertTrue(result.description.contains("金币"));
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
