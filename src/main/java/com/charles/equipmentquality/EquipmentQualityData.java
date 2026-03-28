package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
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

        if (!stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).equals(ItemLore.EMPTY)) {
            return;
        }

        tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".quality", quality.displayName()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".bonus", Component.literal(quality.signedPercent()).withStyle(quality.color())).withStyle(ChatFormatting.DARK_GRAY));
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
        applyQualityModifiers(stack, quality);
        applyQualityLore(stack, quality);
    }

    public static void copyQuality(ItemStack source, ItemStack target) {
        EquipmentQuality quality = getQuality(source);
        if (quality == null || !isSupported(target) || getQuality(target) != null) {
            return;
        }

        setQuality(target, quality);
    }

    private static void applyQualityModifiers(ItemStack stack, EquipmentQuality quality) {
        ItemAttributeModifiers baseModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double factor = 1.0D + quality.multiplierBonus();
        if (factor <= 0.0D) {
            return;
        }

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        boolean changed = false;
        for (ItemAttributeModifiers.Entry entry : baseModifiers.modifiers()) {
            AttributeModifier scaledModifier = scaleModifier(entry.attribute().value(), entry.modifier(), factor);
            builder.add(entry.attribute(), scaledModifier != null ? scaledModifier : entry.modifier(), entry.slot());
            changed |= scaledModifier != null;
        }

        if (changed) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build().withTooltip(baseModifiers.showInTooltip()));
        }
    }

    private static void applyQualityLore(ItemStack stack, EquipmentQuality quality) {
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".quality", quality.displayName()).withStyle(ChatFormatting.DARK_GRAY),
            Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".bonus", Component.literal(quality.signedPercent()).withStyle(quality.color())).withStyle(ChatFormatting.DARK_GRAY)
        )));
    }

    @Nullable
    private static AttributeModifier scaleModifier(Attribute attribute, AttributeModifier modifier, double factor) {
        double adjustedAmount = modifier.amount();

        if (attribute == Attributes.ATTACK_DAMAGE.value()
            || attribute == Attributes.ATTACK_KNOCKBACK.value()
            || attribute == Attributes.ARMOR.value()
            || attribute == Attributes.ARMOR_TOUGHNESS.value()
            || attribute == Attributes.KNOCKBACK_RESISTANCE.value()
            || attribute == Attributes.MINING_EFFICIENCY.value()
            || attribute == Attributes.BLOCK_BREAK_SPEED.value()) {
            adjustedAmount *= factor;
        } else if (attribute == Attributes.ATTACK_SPEED.value()) {
            adjustedAmount = adjustedAmount < 0.0D ? adjustedAmount / factor : adjustedAmount * factor;
        } else {
            return null;
        }

        if (Double.compare(adjustedAmount, modifier.amount()) == 0) {
            return null;
        }

        return new AttributeModifier(modifier.id(), adjustedAmount, modifier.operation());
    }
}