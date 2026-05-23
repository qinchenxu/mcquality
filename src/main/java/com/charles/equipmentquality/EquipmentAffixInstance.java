package com.charles.equipmentquality;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public record EquipmentAffixInstance(String id, String tier, double value, String unit, int displayOrder, String category) {
    private static final String ID_TAG = "id";
    private static final String TIER_TAG = "tier";
    private static final String VALUE_TAG = "value";
    private static final String UNIT_TAG = "unit";
    private static final String DISPLAY_ORDER_TAG = "display_order";
    private static final String CATEGORY_TAG = "category";

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID_TAG, id);
        tag.putString(TIER_TAG, tier);
        tag.putDouble(VALUE_TAG, value);
        tag.putString(UNIT_TAG, unit);
        tag.putInt(DISPLAY_ORDER_TAG, displayOrder);
        tag.putString(CATEGORY_TAG, category);
        return tag;
    }

    @Nullable
    public static EquipmentAffixInstance fromTag(CompoundTag tag) {
        if (!tag.contains(ID_TAG)) {
            return null;
        }

        return new EquipmentAffixInstance(
            tag.getString(ID_TAG),
            tag.getString(TIER_TAG),
            tag.getDouble(VALUE_TAG),
            tag.getString(UNIT_TAG),
            tag.getInt(DISPLAY_ORDER_TAG),
            tag.getString(CATEGORY_TAG)
        );
    }
}