package com.fabricatedbook.core.action;

import com.fabricatedbook.core.buff.*;

/**
 * BuffResolver — Buff 名称解析工具类
 * <p>
 * 将字符串形式的 Buff 名称映射到具体的 BuffFactory 实现。
 * 供 ApplyBuffAction 的便捷构造器使用。
 * <p>
 * 引用方：ApplyBuffAction（便捷构造器）
 */
public final class BuffResolver {

    private BuffResolver() {}

    /**
     * 根据 Buff 名称解析出对应的 BuffFactory。
     * <p>
     * 支持的 Buff 名称与 game_encyclopedia.md 一致。
     *
     * @param buffName Buff 名称（不区分大小写）
     * @return BuffFactory 实例，未知名称返回默认 Strength
     */
    public static ApplyBuffAction.BuffFactory resolve(String buffName) {
        if (buffName == null) {
            return s -> new Strength(s);
        }

        switch (buffName.toLowerCase()) {
            case "fragile":
            case "脆弱":
                return Fragile::new;
            case "block_reduction":
            case "blockreduction":
            case "易碎":
                return BlockReduction::new;
            case "weak":
            case "虚弱":
                return Weak::new;
            case "dizziness":
            case "dizzy":
            case "眩晕":
                return Dizziness::new;
            case "poison":
            case "poisoning":
            case "中毒":
                return Poison::new;
            case "withering":
            case "凋零":
                return Withering::new;
            case "strength":
            case "力量":
                return Strength::new;
            case "resistance":
            case "抗性":
                return Resistance::new;
            case "block_increase":
            case "blockincrease":
            case "坚强":
                return BlockIncrease::new;
            case "armor":
            case "装甲":
                return ArmorBuff::new;
            case "undead":
            case "不死":
                return UndeadBuff::new;
            case "extra_energy":
            case "extraenergy":
            case "额外能量":
                return ExtraEnergyBuff::new;
            default:
                System.out.println("[BuffResolver] 未知 Buff 名称: " + buffName + "，使用默认 Strength");
                return Strength::new;
        }
    }
}
