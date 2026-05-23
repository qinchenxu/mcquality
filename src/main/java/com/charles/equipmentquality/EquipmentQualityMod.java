package com.charles.equipmentquality;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EquipmentQualityMod.MOD_ID)
public final class EquipmentQualityMod {
    public static final String MOD_ID = "equipmentquality";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EquipmentQualityMod(IEventBus modEventBus) {
        ModLootModifiers.GLOBAL_LOOT_MODIFIERS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModNetwork.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(QualityEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(QualityEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(DataDrivenDefinitionReloaders::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(ActiveSkillEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(ActiveSkillEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ActiveSkillEvents::onEntityInteract);
    }
}