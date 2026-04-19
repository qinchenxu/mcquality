package com.charles.equipmentquality;

import net.minecraft.network.chat.Component;

public enum EquipmentActiveSkill {
    ARC_SLASH("arc_slash", 80, "right_click", "arc", 1.6D),
    GUARD_PULSE("guard_pulse", 120, "right_click", "shield", 6.0D),
    SHOCK_BURST("shock_burst", 100, "right_click", "burst", 2.4D);

    private final String id;
    private final int cooldownTicks;
    private final String triggerId;
    private final String particleStyleId;
    private final double primaryValue;

    EquipmentActiveSkill(String id, int cooldownTicks, String triggerId, String particleStyleId, double primaryValue) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
        this.triggerId = triggerId;
        this.particleStyleId = particleStyleId;
        this.primaryValue = primaryValue;
    }

    public String id() {
        return id;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String triggerId() {
        return triggerId;
    }

    public String particleStyleId() {
        return particleStyleId;
    }

    public double primaryValue() {
        return primaryValue;
    }

    public Component displayName() {
        return Component.translatable("skill." + EquipmentQualityMod.MOD_ID + "." + id);
    }

    public Component description() {
        return Component.translatable("skill_desc." + EquipmentQualityMod.MOD_ID + "." + id, formatPrimaryValue());
    }

    public Component triggerName() {
        return Component.translatable("skill_trigger." + EquipmentQualityMod.MOD_ID + "." + triggerId);
    }

    public Component particleStyleName() {
        return Component.translatable("particle_style." + EquipmentQualityMod.MOD_ID + "." + particleStyleId);
    }

    public String formatPrimaryValue() {
        if (this == ARC_SLASH || this == SHOCK_BURST) {
            return String.format(java.util.Locale.ROOT, "%.1f%%", primaryValue * 100.0D);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", primaryValue);
    }

    public static EquipmentActiveSkill byId(String id) {
        for (EquipmentActiveSkill skill : values()) {
            if (skill.id.equals(id)) {
                return skill;
            }
        }
        return null;
    }
}