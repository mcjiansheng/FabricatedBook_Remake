package com.fabricatedbook.core.map;

import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.relic.DataRelic;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.run.GameRunState;

import java.util.Random;

/**
 * Applies core-side effects that happen when a run enters a map node.
 */
public class NodeEntryResolver {
    private static final int DEFAULT_OLIGARCH_GOLD = 20;

    public NodeEntryResult enterNode(GameRunState runState, NodeType nodeType) {
        GameRunState.NodeRef nodeRef = nodeType == null ? null
                : new GameRunState.NodeRef(runState != null ? runState.getCurrentLayerIdx() : 0,
                -1, -1, typeCode(nodeType));
        return enterNode(runState, nodeRef);
    }

    public NodeEntryResult enterNode(GameRunState runState, GameRunState.NodeRef nodeRef) {
        NodeEntryResult result = new NodeEntryResult();
        if (runState == null || nodeRef == null) {
            return result;
        }
        NodeType nodeType = LayerMapGraph.fromTypeCode(nodeRef.type);
        Player player = runState.getPlayer();
        if (player == null) {
            return result;
        }

        applyEnvironment(runState, player, nodeRef, nodeType, result);
        applyOligarch(player, nodeType, result);
        return result;
    }

    private void applyEnvironment(GameRunState runState, Player player,
                                  GameRunState.NodeRef nodeRef, NodeType nodeType,
                                  NodeEntryResult result) {
        Random random = runState.randomFor("node-entry", nodeRef.layer, nodeRef.col,
                nodeRef.row, nodeRef.type);
        if (nodeRef.layer == 1 && !nodeType.isCombat()) {
            int amount = 10 + random.nextInt(11);
            int actualLost = Math.min(player.getGold(), amount);
            player.setGold(player.getGold() - amount);
            result.loseGold(actualLost, "森林环境：失去 " + actualLost + " 金币");
        } else if (nodeRef.layer == 2) {
            int before = runState.getMapDamageModifier();
            runState.addMapDamageModifier(nodeType.isCombat() ? -1 : 1);
            int change = runState.getMapDamageModifier() - before;
            if (change != 0) {
                result.changeDamageModifier(change,
                        "诡异秘林环境：伤害修正 " + signed(runState.getMapDamageModifier()));
            }
        } else if (nodeRef.layer == 3) {
            if (random.nextInt(2) == 0) {
                int amount = 5 + random.nextInt(26);
                int healed = player.heal(amount);
                result.heal(healed, "迷雾环境：回复 " + healed + " 生命值");
            } else {
                int amount = 1 + random.nextInt(20);
                int lost = player.takeDamage(amount);
                result.loseHp(lost, "迷雾环境：失去 " + lost + " 生命值");
            }
        }
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
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

    private int typeCode(NodeType type) {
        return switch (type) {
            case FIGHT -> 1;
            case EMERGENCY -> 2;
            case BOSS -> 3;
            case UNEXPECTED -> 4;
            case REWARD -> 5;
            case SHOP -> 6;
            case DECISION -> 8;
            case SAFEHOUSE -> 9;
        };
    }
}
