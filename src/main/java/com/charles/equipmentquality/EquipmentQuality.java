package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum EquipmentQuality {
    WORN("worn", -0.20D, ChatFormatting.GRAY, 18, 0, 1, 0.0D, 0),
    NORMAL("normal", 0.10D, ChatFormatting.WHITE, 30, 1, 2, 0.0D, 1),
    UNCOMMON("uncommon", 0.30D, ChatFormatting.GREEN, 22, 2, 3, 0.08D, 2),
    RARE("rare", 0.50D, ChatFormatting.AQUA, 16, 3, 4, 0.15D, 3),
    EPIC("epic", 0.80D, ChatFormatting.LIGHT_PURPLE, 9, 4, 5, 0.30D, 4),
    LEGENDARY("legendary", 1.00D, ChatFormatting.GOLD, 5, 5, 6, 0.45D, 5);

    private final String id;
    private final double multiplierBonus;
    private final ChatFormatting color;
    private final int weight;
    private final int minAffixCount;
    private final int maxAffixCount;
    private final double skillChance;
    private final int displayPriority;

    EquipmentQuality(String id, double multiplierBonus, ChatFormatting color, int weight, int minAffixCount, int maxAffixCount, double skillChance, int displayPriority) {
        this.id = id;
        this.multiplierBonus = multiplierBonus;
        this.color = color;
        this.weight = weight;
        this.minAffixCount = minAffixCount;
        this.maxAffixCount = maxAffixCount;
        this.skillChance = skillChance;
        this.displayPriority = displayPriority;
    }

    public String id() {
        return id;
    }

    public double multiplierBonus() {
        return multiplierBonus;
    }

    public ChatFormatting color() {
        return color;
    }

    public int weight() {
        return weight;
    }

    public int minAffixCount() {
        return minAffixCount;
    }

    public int maxAffixCount() {
        return maxAffixCount;
    }

    public double skillChance() {
        return skillChance;
    }

    public int displayPriority() {
        return displayPriority;
    }

    public String translationKey() {
        return "quality." + EquipmentQualityMod.MOD_ID + "." + id;
    }

    public Component displayName() {
        return Component.translatable(translationKey()).withStyle(color);
    }

    public String signedPercent() {
        return String.format(Locale.ROOT, "%+d%%", (int) Math.round(multiplierBonus * 100.0D));
    }

    public static EquipmentQuality byId(String id) {
        for (EquipmentQuality quality : values()) {
            if (quality.id.equals(id)) {
                return quality;
            }
        }
        return null;
    }
}