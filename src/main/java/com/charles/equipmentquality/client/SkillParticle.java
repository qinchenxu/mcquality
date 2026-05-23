package com.charles.equipmentquality.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class SkillParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private SkillParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        SpriteSet sprites,
        float red,
        float green,
        float blue,
        float scale,
        int lifetime
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.88F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.quadSize *= scale;
        this.setColor(red, green, blue);
        this.setAlpha(0.92F);
        this.setLifetime(lifetime + this.random.nextInt(4));
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        this.xd *= 0.92D;
        this.yd *= 0.92D;
        this.zd *= 0.92D;
        this.setAlpha(Math.max(0.0F, 0.92F - ((float) this.age / this.lifetime)));
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 240;
    }

    public static ParticleEngine.SpriteParticleRegistration<SimpleParticleType> registration(float red, float green, float blue, float scale, int lifetime) {
        return sprites -> new Provider(sprites, red, green, blue, scale, lifetime);
    }

    private record Provider(SpriteSet sprites, float red, float green, float blue, float scale, int lifetime) implements ParticleProvider<SimpleParticleType> {
        @Override
        public SkillParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SkillParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, red, green, blue, scale, lifetime);
        }
    }
}