package com.fabricatedbook.core.relic;

/**
 * RelicData — JSON 藏品数据传输对象。
 */
public class RelicData {
    private String id;
    private String name;
    private Relic.Rarity rarity;
    private String description;
    private int effectValue;

    public String getId() { return id; }
    public String getName() { return name; }
    public Relic.Rarity getRarity() { return rarity; }
    public String getDescription() { return description; }
    public int getEffectValue() { return effectValue; }
}
