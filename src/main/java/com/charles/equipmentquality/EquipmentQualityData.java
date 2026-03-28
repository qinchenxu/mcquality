package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EquipmentQualityData {
    private static final String QUALITY_TAG = "EquipmentQuality";

    private EquipmentQualityData() {
    }

    public static boolean isSupported(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem || stack.getItem() instanceof TieredItem;
    }

    public static void assignRandomQuality(ItemStack stack, RandomSource random) {
        if (!isSupported(stack) || getQuality(stack) != null) {
            return;
        }

        int totalWeight = 0;
        for (EquipmentQuality quality : EquipmentQuality.values()) {
            totalWeight += quality.weight();
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (EquipmentQuality quality : EquipmentQuality.values()) {
            current += quality.weight();
            if (roll < current) {
                setQuality(stack, quality);
                return;
            }
        }
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        EquipmentQuality quality = getQuality(stack);
        if (quality == null) {
            return;
        }

        tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".quality", quality.displayName()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".bonus", Component.literal(quality.signedPercent()).withStyle(quality.color())).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void applyAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentQuality quality = getQuality(stack);
        if (quality == null) {
            return;
        }

        double amount = quality.multiplierBonus();
        EquipmentSlot slot = event.getSlotType();

        if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot) {
            event.addModifier(Attributes.ARMOR, new AttributeModifier(EquipmentQualityMod.MOD_ID + ".armor", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(EquipmentQualityMod.MOD_ID + ".armor_toughness", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(EquipmentQualityMod.MOD_ID + ".knockback_resistance", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (stack.getItem() instanceof TieredItem && slot == EquipmentSlot.MAINHAND) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(EquipmentQualityMod.MOD_ID + ".attack_damage", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(EquipmentQualityMod.MOD_ID + ".attack_speed", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Nullable
    public static EquipmentQuality getQuality(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(QUALITY_TAG)) {
            return null;
        }

        return EquipmentQuality.byId(tag.getString(QUALITY_TAG));
    }

    private static void setQuality(ItemStack stack, EquipmentQuality quality) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null) {
            tag = new CompoundTag();
        }

        tag.putString(QUALITY_TAG, quality.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}