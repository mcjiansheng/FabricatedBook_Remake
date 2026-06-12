package com.fabricatedbook.core.entity;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.card.Card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AbstractEntity — 实体基类
 * <p>
 * 所有战斗实体（玩家、敌人）的抽象基类。
 * 持有生命值、最大生命值、格挡值、能量、Buff 列表、手牌/抽牌堆/弃牌堆等核心属性。
 * 提供通用的 getter/setter 方法，子类可在此基础上扩展。
 * <p>
 * 引用方：Player（继承）、Enemy（继承）、DamageCalculator（读取 HP/Block/Buff 列表）、
 *         CombatEngine（操作实体状态）、ActionManager（执行动作时操作实体）
 */
public abstract class AbstractEntity {

    /** 实体唯一标识符 */
    protected String id;

    /** 实体显示名称 */
    protected String name;

    /** 当前生命值 */
    protected int hp;

    /** 最大生命值 */
    protected int maxHp;

    /** 当前格挡值 */
    protected int block;

    /** 当前能量（仅玩家使用，敌人能量用于某些特定逻辑） */
    protected int energy;

    /** 每回合最大能量（初始为 3） */
    protected int maxEnergy;

    /** 当前持有的 Buff 列表 */
    protected List<BuffHook> buffs;

    /** 手牌列表 */
    protected List<Card> hand;

    /** 抽牌堆 */
    protected List<Card> drawPile;

    /** 弃牌堆 */
    protected List<Card> discardPile;

    /** 是否存活 */
    protected boolean alive;

    /** 是否眩晕 */
    protected boolean dizzy;

    /**
     * 构造实体基类。
     *
     * @param id       实体唯一标识符
     * @param name     实体显示名称
     * @param maxHp    最大生命值
     */
    protected AbstractEntity(String id, String name, int maxHp) {
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.block = 0;
        this.energy = 0;
        this.maxEnergy = 3;
        this.buffs = new ArrayList<>();
        this.hand = new ArrayList<>();
        this.drawPile = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.alive = true;
        this.dizzy = false;
    }

    // ====== 生命值相关 ======

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getHp() { return hp; }

    /**
     * 设置当前生命值，确保不超过最大生命值且不低于 0。
     *
     * @param hp 新的生命值
     */
    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(hp, maxHp));
        if (this.hp <= 0) {
            this.alive = false;
        }
    }

    public int getMaxHp() { return maxHp; }

    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    /**
     * 增加最大生命值上限。
     *
     * @param amount 增加量
     */
    public void addMaxHp(int amount) {
        this.maxHp += amount;
    }

    /**
     * 受到伤害，优先扣除格挡再扣生命值。
     *
     * @param damage 原始伤害值（未经过 Buff 修正）
     * @return 实际扣除的生命值
     */
    public int takeDamage(int damage) {
        int remaining = damage;
        // 先扣除格挡
        if (block > 0) {
            if (block >= remaining) {
                block -= remaining;
                remaining = 0;
            } else {
                remaining -= block;
                block = 0;
            }
        }
        // 再扣除生命值
        if (remaining > 0) {
            int oldHp = hp;
            setHp(hp - remaining);
            return oldHp - hp;
        }
        return 0;
    }

    /**
     * 回复生命值。
     *
     * @param amount 回复量
     * @return 实际回复的生命值
     */
    public int heal(int amount) {
        int oldHp = hp;
        setHp(hp + amount);
        return hp - oldHp;
    }

    // ====== 格挡相关 ======

    public int getBlock() { return block; }

    public void setBlock(int block) { this.block = Math.max(0, block); }

    /**
     * 获得格挡值。
     *
     * @param amount 格挡获得量
     */
    public void gainBlock(int amount) {
        this.block += Math.max(0, amount);
    }

    /**
     * 清除格挡值（通常回合结束时调用）。
     */
    public void clearBlock() {
        this.block = 0;
    }

    // ====== 能量相关 ======

    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = Math.max(0, energy); }

    public int getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(int maxEnergy) { this.maxEnergy = Math.max(1, maxEnergy); }

    /**
     * 增加能量（通常通过 GainEnergyAction）。
     *
     * @param amount 增加的能量数
     */
    public void gainEnergy(int amount) {
        this.energy += Math.max(0, amount);
    }

    /**
     * 消耗能量（使用卡牌时调用）。
     *
     * @param cost 消耗量
     * @return 是否成功消耗
     */
    public boolean spendEnergy(int cost) {
        if (energy >= cost) {
            energy -= cost;
            return true;
        }
        return false;
    }

    // ====== Buff 相关 ======

    public List<BuffHook> getBuffs() { return buffs; }

    /**
     * 添加 Buff 到实体。
     *
     * @param buff 要添加的 BuffHook 实例
     */
    public void addBuff(BuffHook buff) {
        this.buffs.add(buff);
    }

    /**
     * 移除指定名称的 Buff。
     *
     * @param buffName Buff 名称
     */
    public void removeBuff(String buffName) {
        buffs.removeIf(b -> b.getBuffName().equals(buffName));
    }

    /**
     * 检查是否包含指定名称的 Buff。
     *
     * @param buffName Buff 名称
     * @return true 如果存在该 Buff
     */
    public boolean hasBuff(String buffName) {
        return buffs.stream().anyMatch(b -> b.getBuffName().equals(buffName));
    }

    /**
     * 移除所有层数为 0 的 Buff。
     */
    public void cleanExpiredBuffs() {
        Iterator<BuffHook> it = buffs.iterator();
        while (it.hasNext()) {
            if (it.next().getStack() <= 0) {
                it.remove();
            }
        }
    }

    // ====== 手牌 / 牌堆相关 ======

    public List<Card> getHand() { return hand; }
    public void setHand(List<Card> hand) { this.hand = hand; }

    public List<Card> getDrawPile() { return drawPile; }
    public void setDrawPile(List<Card> drawPile) { this.drawPile = drawPile; }

    public List<Card> getDiscardPile() { return discardPile; }
    public void setDiscardPile(List<Card> discardPile) { this.discardPile = discardPile; }

    /**
     * 从抽牌堆抽指定数量的牌到手牌。
     * 如果抽牌堆不足，将弃牌堆洗回抽牌堆。
     *
     * @param count 抽牌数量
     * @return 实际抽到的牌数
     */
    public int drawCards(int count) {
        int drawn = 0;
        for (int i = 0; i < count; i++) {
            if (drawPile.isEmpty()) {
                shuffleDiscardToDraw();
                if (drawPile.isEmpty()) break;
            }
            Card card = drawPile.remove(drawPile.size() - 1);
            hand.add(card);
            drawn++;
        }
        return drawn;
    }

    /**
     * 将弃牌堆洗回抽牌堆。
     */
    public void shuffleDiscardToDraw() {
        drawPile.addAll(discardPile);
        discardPile.clear();
        java.util.Collections.shuffle(drawPile);
    }

    /**
     * 将手牌弃入弃牌堆。
     */
    public void discardHand() {
        discardPile.addAll(hand);
        hand.clear();
    }

    // ====== 状态相关 ======

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isDizzy() { return dizzy; }
    public void setDizzy(boolean dizzy) { this.dizzy = dizzy; }

    @Override
    public String toString() {
        return name + "[HP:" + hp + "/" + maxHp + " Block:" + block + " Energy:" + energy + "]";
    }
}
