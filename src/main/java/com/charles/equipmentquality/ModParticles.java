package com.charles.equipmentquality;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, EquipmentQualityMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARC_SLASH = PARTICLE_TYPES.register("arc_slash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUARD_PULSE = PARTICLE_TYPES.register("guard_pulse", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_BURST = PARTICLE_TYPES.register("shock_burst", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}