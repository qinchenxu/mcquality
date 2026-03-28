package com.charles.equipmentquality;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EquipmentQualityMod.MOD_ID)
public final class EquipmentQualityMod {
    public static final String MOD_ID = "equipmentquality";

    public EquipmentQualityMod(FMLJavaModLoadingContext context) {
        ModLootModifiers.GLOBAL_LOOT_MODIFIERS.register(context.getModBusGroup());

        PlayerEvent.ItemCraftedEvent.BUS.addListener(QualityEvents::onItemCrafted);
        ItemTooltipEvent.BUS.addListener(QualityEvents::onItemTooltip);
        MinecraftForge.EVENT_BUS.addListener(QualityEvents::onItemAttribute);
    }
}