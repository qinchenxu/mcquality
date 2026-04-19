package com.charles.equipmentquality;

import net.minecraft.network.chat.Component;

public enum EquipmentPassiveEffect {
    STEADY_EDGE("steady_edge", 6.0D, true),
    SWIFT_STRIKE("swift_strike", 4.0D, true),
    TITAN_GRIP("titan_grip", 10.0D, true),
    GUARD_BREAKER("guard_breaker", 12.0D, true);

    private final String id;
    private final double value;
    private final boolean percent;

    EquipmentPassiveEffect(String id, double value, boolean percent) {
        this.id = id;
        this.value = value;
        this.percent = percent;
    }

    public String id() {
        return id;
    }

    public double value() {
        return value;
    }

    public boolean percent() {
        return percent;
    }

    public Component displayName() {
        return Component.translatable("passive." + EquipmentQualityMod.MOD_ID + "." + id);
    }

    public Component description() {
        return Component.translatable("passive_desc." + EquipmentQualityMod.MOD_ID + "." + id, formatValue());
    }

    public String formatValue() {
        if (percent) {
            return String.format(java.util.Locale.ROOT, "%.1f%%", value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public static EquipmentPassiveEffect byId(String id) {
        for (EquipmentPassiveEffect passiveEffect : values()) {
            if (passiveEffect.id.equals(id)) {
                return passiveEffect;
            }
        }
        return null;
    }
}