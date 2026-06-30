package com.fabricatedbook.core.run;

import org.junit.jupiter.api.Test;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GameRunStateRandomTest {

    @Test
    void sameSeedAndKeyCreateSameRandomSequence() {
        Random first = GameRunState.randomFor(12345L, "map:0:node:1:2");
        Random second = GameRunState.randomFor(12345L, "map:0:node:1:2");

        assertEquals(first.nextLong(), second.nextLong());
        assertEquals(first.nextInt(), second.nextInt());
        assertEquals(first.nextDouble(), second.nextDouble());
    }

    @Test
    void differentKeysAndPartsCreateIndependentSeeds() {
        long baseSeed = 12345L;

        assertNotEquals(GameRunState.seedFor(baseSeed, "event-name:1:2"),
                GameRunState.seedFor(baseSeed, "event-result:1:2"));
        assertNotEquals(GameRunState.seedFor(baseSeed, "shop:cards"),
                GameRunState.seedFor(baseSeed, "shop:relics"));
        Random partsKey = new GameRunState(baseSeed, warrior())
                .randomFor("reward", 1, 2, "card-pick");
        Random joinedKey = GameRunState.randomFor(baseSeed, "reward:1:2:card-pick");
        assertEquals(partsKey.nextLong(), joinedKey.nextLong());
    }

    @Test
    void consumingOneRandomStreamDoesNotAdvanceAnotherStream() {
        long seed = 987654321L;
        Random eventName = GameRunState.randomFor(seed, "event-name:2:0");
        eventName.nextInt();
        eventName.nextInt();

        Random firstEventResult = GameRunState.randomFor(seed, "event-result:2:0");
        Random secondEventResult = GameRunState.randomFor(seed, "event-result:2:0");

        assertEquals(firstEventResult.nextInt(), secondEventResult.nextInt());
        assertEquals(firstEventResult.nextLong(), secondEventResult.nextLong());
    }

    private static Player warrior() {
        return new Player("random-test", "随机测试", Profession.WARRIOR);
    }
}
