package com.charles.equipmentquality;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EquipmentQualityMod.MOD_ID)
public final class EquipmentQualityMod {
    public static final String MOD_ID = "equipmentquality";

    public EquipmentQualityMod(FMLJavaModLoadingContext context) {
        ModLootModifiers.GLOBAL_LOOT_MODIFIERS.register(context.getModEventBus());

        MinecraftForge.EVENT_BUS.addListener(QualityEvents::onItemCrafted);
        MinecraftForge.EVENT_BUS.addListener(QualityEvents::onItemTooltip);
    }
}