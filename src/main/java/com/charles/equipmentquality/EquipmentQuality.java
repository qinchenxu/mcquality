package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum EquipmentQuality {
    WORN("worn", -0.20D, ChatFormatting.GRAY, 18),
    NORMAL("normal", 0.10D, ChatFormatting.WHITE, 30),
    UNCOMMON("uncommon", 0.30D, ChatFormatting.GREEN, 22),
    RARE("rare", 0.50D, ChatFormatting.AQUA, 16),
    EPIC("epic", 0.80D, ChatFormatting.LIGHT_PURPLE, 9),
    LEGENDARY("legendary", 1.00D, ChatFormatting.GOLD, 5);

    private final String id;
    private final double multiplierBonus;
    private final ChatFormatting color;
    private final int weight;

    EquipmentQuality(String id, double multiplierBonus, ChatFormatting color, int weight) {
        this.id = id;
        this.multiplierBonus = multiplierBonus;
        this.color = color;
        this.weight = weight;
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