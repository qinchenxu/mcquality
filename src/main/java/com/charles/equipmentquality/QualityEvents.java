package com.charles.equipmentquality;

import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class QualityEvents {
    private QualityEvents() {
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        EquipmentQualityData.assignRandomQuality(event.getCrafting(), event.getEntity().getRandom());
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        EquipmentQualityData.appendTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void onItemAttribute(ItemAttributeModifierEvent event) {
        EquipmentQualityData.applyAttributeModifiers(event);
    }
}