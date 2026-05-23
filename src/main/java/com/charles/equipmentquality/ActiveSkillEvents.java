package com.charles.equipmentquality;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class ActiveSkillEvents {
    private static final double ARC_SLASH_RANGE = 3.5D;
    private static final double ARC_SLASH_DOT = 0.35D;
    private static final double GUARD_PULSE_RADIUS = 3.0D;
    private static final double SHOCK_BURST_RADIUS = 3.5D;

    private ActiveSkillEvents() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryUseActiveSkill(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryUseActiveSkill(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (tryUseActiveSkill(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean tryUseActiveSkill(Player player, InteractionHand hand) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof TieredItem)) {
            return false;
        }

        EquipmentActiveSkill skill = EquipmentQualityData.getActiveSkill(stack);
        if (skill == null || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        boolean executed = switch (skill) {
            case ARC_SLASH -> executeArcSlash(serverLevel, player, skill);
            case GUARD_PULSE -> executeGuardPulse(serverLevel, player, skill);
            case SHOCK_BURST -> executeShockBurst(serverLevel, player, skill);
        };

        if (!executed) {
            return false;
        }

        player.getCooldowns().addCooldown(stack.getItem(), skill.cooldownTicks());
        player.swing(hand, true);
        return true;
    }

    private static boolean executeArcSlash(ServerLevel level, Player player, EquipmentActiveSkill skill) {
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
        Vec3 look = player.getLookAngle().normalize();
        float damage = getSkillDamage(player, skill);

        for (LivingEntity target : getTargets(level, player, ARC_SLASH_RANGE)) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 offset = targetCenter.subtract(origin);
            if (offset.lengthSqr() > ARC_SLASH_RANGE * ARC_SLASH_RANGE || offset.lengthSqr() < 0.01D) {
                continue;
            }

            if (look.dot(offset.normalize()) < ARC_SLASH_DOT) {
                continue;
            }

            target.hurt(level.damageSources().playerAttack(player), damage);
            pushTarget(target, look.scale(0.65D), 0.12D);
        }

        for (int step = 1; step <= 6; step++) {
            Vec3 point = origin.add(look.scale(step * 0.55D));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.95F);
        return true;
    }

    private static boolean executeGuardPulse(ServerLevel level, Player player, EquipmentActiveSkill skill) {
        player.setAbsorptionAmount(player.getAbsorptionAmount() + (float) skill.primaryValue());

        for (LivingEntity target : getTargets(level, player, GUARD_PULSE_RADIUS)) {
            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() < 0.01D) {
                continue;
            }

            pushTarget(target, push.normalize().scale(0.95D), 0.24D);
        }

        spawnPulse(level, player, ParticleTypes.END_ROD, 14, 1.3D);
        spawnPulse(level, player, ParticleTypes.GLOW, 10, 0.8D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    private static boolean executeShockBurst(ServerLevel level, Player player, EquipmentActiveSkill skill) {
        float damage = getSkillDamage(player, skill);

        for (LivingEntity target : getTargets(level, player, SHOCK_BURST_RADIUS)) {
            target.hurt(level.damageSources().playerAttack(player), damage);

            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() > 0.01D) {
                pushTarget(target, push.normalize().scale(1.2D), 0.28D);
            }
        }

        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.7D, 0.25D, 0.7D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.8D, 0.35D, 0.8D, 0.15D);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    private static List<LivingEntity> getTargets(ServerLevel level, Player player, double radius) {
        AABB searchBox = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> target != player
            && target.isAlive()
            && !target.isSpectator()
            && !target.isAlliedTo(player)
            && player.hasLineOfSight(target));
    }

    private static float getSkillDamage(Player player, EquipmentActiveSkill skill) {
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return (float) Math.max(1.0D, attackDamage * skill.primaryValue());
    }

    private static void pushTarget(LivingEntity target, Vec3 direction, double verticalBoost) {
        target.push(direction.x, verticalBoost, direction.z);
        target.hurtMarked = true;
    }

    private static void spawnPulse(ServerLevel level, Player player, ParticleOptions particle, int points, double radius) {
        double y = player.getY() + player.getBbHeight() * 0.6D;
        for (int index = 0; index < points; index++) {
            double angle = (Math.PI * 2.0D * index) / points;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}