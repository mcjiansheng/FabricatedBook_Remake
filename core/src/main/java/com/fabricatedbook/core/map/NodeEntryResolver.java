package com.fabricatedbook.core.map;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.DataRelic;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.run.GameRunState;

/**
 * Applies core-side effects that happen when a run enters a map node.
 */
public class NodeEntryResolver {
    private static final int DEFAULT_OLIGARCH_GOLD = 20;

    public NodeEntryResult enterNode(GameRunState runState, NodeType nodeType) {
        NodeEntryResult result = new NodeEntryResult();
        if (runState == null || nodeType == null) {
            return result;
        }
        Player player = runState.getPlayer();
        if (player == null) {
            return result;
        }

        applyOligarch(player, nodeType, result);
        return result;
    }

    private void applyOligarch(Player player, NodeType nodeType, NodeEntryResult result) {
        if (nodeType.isCombat()) {
            return;
        }
        Relic oligarch = findRelic(player, "relic_oligarch");
        if (oligarch == null) {
            return;
        }
        int amount = oligarch instanceof DataRelic
                ? ((DataRelic) oligarch).getEffectValue()
                : DEFAULT_OLIGARCH_GOLD;
        player.gainGold(amount);
        result.gainGold(amount, oligarch.getName() + "：获得 " + amount + " 金币");
    }

    private Relic findRelic(Player player, String relicId) {
        for (Relic relic : player.getRelics()) {
            if (relicId.equals(relic.getId())) {
                return relic;
            }
        }
        return null;
    }
}
