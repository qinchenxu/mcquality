package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityData;
import com.charles.equipmentquality.EquipmentQualityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = EquipmentQualityMod.MOD_ID, value = Dist.CLIENT)
public final class EquipmentQualityClientGameEvents {
    private EquipmentQualityClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (ModKeyMappings.OPEN_DETAILS.consumeClick()) {
            ItemStack stack = minecraft.player.getMainHandItem();
            if (EquipmentQualityData.supportsDetailsPanel(stack)) {
                minecraft.setScreen(new EquipmentDetailsScreen(stack));
            }
        }
    }
}