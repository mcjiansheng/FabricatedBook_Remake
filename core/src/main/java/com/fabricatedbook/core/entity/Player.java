package com.fabricatedbook.core.entity;

import com.fabricatedbook.core.relic.Relic;

import java.util.ArrayList;
import java.util.List;

/**
 * Player — 玩家实体
 * <p>
 * 继承自 AbstractEntity，代表玩家角色。
 * 额外持有职业类型、金币和已获得的藏品列表。
 * 战士职业特性：战斗胜利后回复 6-12 点生命值。
 * <p>
 * 引用方：CombatEngine（管理玩家状态）、ShopManager（扣除金币）、
 *         RelicManager（管理玩家藏品）
 */
public class Player extends AbstractEntity {

    /** 玩家职业 */
    private Profession profession;

    /** 当前持有的金币 */
    private int gold;

    /** 已获得的藏品列表 */
    private List<Relic> relics;

    /** 当前楼层编号（1-5） */
    private int currentFloor;

    /** 战斗胜利累积的卡牌数量 */
    private int cardCount;

    /**
     * 构造玩家实体。
     *
     * @param id         玩家 ID
     * @param name       玩家名称
     * @param profession 职业
     */
    public Player(String id, String name, Profession profession) {
        super(id, name, profession.getBaseMaxHp());
        this.profession = profession;
        this.gold = 0;
        this.relics = new ArrayList<>();
        this.currentFloor = 1;
        this.cardCount = 0;
    }

    // ====== 职业相关 ======

    public Profession getProfession() { return profession; }
    public void setProfession(Profession profession) { this.profession = profession; }

    // ====== 金币相关 ======

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = Math.max(0, gold); }

    /**
     * 增加金币。
     *
     * @param amount 增加数量
     */
    public void gainGold(int amount) {
        this.gold += Math.max(0, amount);
    }

    /**
     * 消耗金币。
     *
     * @param amount 消耗数量
     * @return true 如果金币足够并成功扣除
     */
    public boolean spendGold(int amount) {
        if (gold >= amount) {
            gold -= amount;
            return true;
        }
        return false;
    }

    // ====== 藏品相关 ======

    public List<Relic> getRelics() { return relics; }
    public void setRelics(List<Relic> relics) { this.relics = relics; }

    /**
     * 添加藏品。
     *
     * @param relic 要添加的藏品
     */
    public void addRelic(Relic relic) {
        this.relics.add(relic);
    }

    /**
     * 检查是否已持有指定藏品。
     *
     * @param relicName 藏品名称
     * @return true 如果已持有
     */
    public boolean hasRelic(String relicName) {
        return relics.stream().anyMatch(r -> r.getName().equals(relicName));
    }

    // ====== 楼层相关 ======

    public int getCurrentFloor() { return currentFloor; }
    public void setCurrentFloor(int currentFloor) { this.currentFloor = currentFloor; }

    public int getCardCount() { return cardCount; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }

    /**
     * 战斗胜利后回复生命值（战士特性）。
     *
     * @return 实际回复量
     */
    public int battleRewardHeal() {
        if (profession == Profession.WARRIOR) {
            int healAmount = 6 + (int)(Math.random() * 7); // 6-12
            return heal(healAmount);
        }
        return 0;
    }
}
