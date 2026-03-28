package com.charles.equipmentquality;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class QualityEvents {
    private QualityEvents() {
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        ItemStack originalCrafted = crafted.copy();

        EquipmentQualityData.assignRandomQuality(crafted, event.getEntity().getRandom());
        syncCraftedStack(event.getEntity(), originalCrafted, crafted);
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        EquipmentQualityData.appendTooltip(event.getItemStack(), event.getToolTip());
    }

    private static void syncCraftedStack(Player player, ItemStack originalCrafted, ItemStack craftedWithQuality) {
        if (EquipmentQualityData.getQuality(craftedWithQuality) == null) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (isMatchingCraftResult(carried, originalCrafted)) {
            EquipmentQualityData.copyQuality(craftedWithQuality, carried);
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);
            if (isMatchingCraftResult(inventoryStack, originalCrafted)) {
                EquipmentQualityData.copyQuality(craftedWithQuality, inventoryStack);
                return;
            }
        }
    }

    private static boolean isMatchingCraftResult(ItemStack candidate, ItemStack originalCrafted) {
        return !candidate.isEmpty()
            && EquipmentQualityData.getQuality(candidate) == null
            && candidate.getCount() == originalCrafted.getCount()
            && ItemStack.isSameItemSameComponents(candidate, originalCrafted);
    }
}