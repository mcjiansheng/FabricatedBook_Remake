package com.fabricatedbook.core.card;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates long-lived starting decks for new runs.
 */
public final class StarterDeckFactory {

    private StarterDeckFactory() {}

    public static void addStarterDeckIfEmpty(Player player) {
        if (player == null || hasAnyCards(player)) return;
        player.getDrawPile().addAll(createStarterDeck(player.getProfession()));
    }

    public static List<Card> createStarterDeck(Profession profession) {
        List<Card> cards = new ArrayList<>();
        for (StarterCardSpec spec : starterSpecs(profession)) {
            Card template = CardPool.findById(spec.cardId);
            if (template == null) continue;
            for (int i = 0; i < spec.count; i++) {
                cards.add(CardFactory.createFromTemplate(template));
            }
        }
        return cards;
    }

    private static List<StarterCardSpec> starterSpecs(Profession profession) {
        if (profession == Profession.WARRIOR) {
            return List.of(
                    new StarterCardSpec("war_atk1", 5),
                    new StarterCardSpec("war_def1", 4),
                    new StarterCardSpec("war_painful_blow", 1)
            );
        }

        // Mage and Witch card pools are not complete yet. Keep them playable
        // with the existing basic deck until their own JSON decks are defined.
        return starterSpecs(Profession.WARRIOR);
    }

    private static boolean hasAnyCards(Player player) {
        return !player.getDrawPile().isEmpty()
                || !player.getHand().isEmpty()
                || !player.getDiscardPile().isEmpty()
                || !player.getExhaustPile().isEmpty();
    }

    private record StarterCardSpec(String cardId, int count) {}
}
