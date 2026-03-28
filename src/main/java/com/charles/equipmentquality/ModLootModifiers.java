package com.charles.equipmentquality;

import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EquipmentQualityMod.MOD_ID);

    public static final RegistryObject<MapCodec<? extends IGlobalLootModifier>> APPLY_QUALITY =
        GLOBAL_LOOT_MODIFIERS.register("apply_quality", () -> QualityLootModifier.CODEC.get());

    private ModLootModifiers() {
    }
}