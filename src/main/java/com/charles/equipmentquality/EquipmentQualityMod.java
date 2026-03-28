package com.charles.equipmentquality;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(EquipmentQualityMod.MOD_ID)
public final class EquipmentQualityMod {
    public static final String MOD_ID = "equipmentquality";

    public EquipmentQualityMod(IEventBus modEventBus) {
        ModLootModifiers.GLOBAL_LOOT_MODIFIERS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(QualityEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(QualityEvents::onItemTooltip);
    }
}