package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityMod;
import com.charles.equipmentquality.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = EquipmentQualityMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EquipmentQualityClientModEvents {
    private EquipmentQualityClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_DETAILS);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ARC_SLASH.get(), SkillParticle.registration(0.95F, 0.84F, 0.38F, 1.25F, 14));
        event.registerSpriteSet(ModParticles.GUARD_PULSE.get(), SkillParticle.registration(0.55F, 0.88F, 0.96F, 1.45F, 18));
        event.registerSpriteSet(ModParticles.SHOCK_BURST.get(), SkillParticle.registration(1.0F, 0.62F, 0.24F, 1.6F, 12));
    }
}