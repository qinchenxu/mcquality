package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EquipmentQualityData {
    private static final String QUALITY_TAG = "EquipmentQuality";
    private static final String ACTIVE_SKILL_TAG = "EquipmentQualityActiveSkill";
    private static final String PASSIVE_EFFECT_TAG = "EquipmentQualityPassiveEffect";

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
        if (supportsDetailsPanel(stack)) {
            tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".details_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean supportsDetailsPanel(ItemStack stack) {
        return stack.getItem() instanceof TieredItem;
    }

    public static List<DetailSection> getDetailSections(ItemStack stack) {
        List<DetailSection> sections = new ArrayList<>();
        EquipmentQuality quality = getQuality(stack);

        List<Component> summaryLines = new ArrayList<>();
        summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.rarity", quality != null ? quality.displayName() : Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.none").withStyle(ChatFormatting.GRAY)));
        summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.type", getEquipmentTypeLabel(stack)));
        summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.attr", quality != null ? Component.literal(quality.signedPercent()).withStyle(quality.color()) : Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.attr.none").withStyle(ChatFormatting.GRAY)));
        sections.add(new DetailSection(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.summary"), summaryLines));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.attributes"),
            getAttributeLines(stack)
        ));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.active_skill"),
            getActiveSkillLines(stack)
        ));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.passive"),
            getPassiveLines(stack)
        ));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.controls"),
            List.of(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.hint"))
        ));

        return sections;
    }

    public record DetailSection(Component title, List<Component> lines) {
    }

    private static List<Component> getAttributeLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        ItemAttributeModifiers attributeModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (attributeModifiers.modifiers().isEmpty()) {
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_attributes").withStyle(ChatFormatting.GRAY));
            return lines;
        }

        for (ItemAttributeModifiers.Entry entry : attributeModifiers.modifiers()) {
            lines.add(formatAttributeLine(entry.attribute().value(), entry.modifier()));
        }

        return lines;
    }

    private static List<Component> getActiveSkillLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        EquipmentActiveSkill skill = getActiveSkill(stack);
        if (skill == null) {
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_active_skill").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill_hint").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill.name", skill.displayName()));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill.desc", skill.description()));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill.trigger", skill.triggerName()));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill.cooldown", formatCooldownSeconds(skill.cooldownTicks())));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.skill.particle", skill.particleStyleName()));
        return lines;
    }

    private static List<Component> getPassiveLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        EquipmentPassiveEffect passiveEffect = getPassiveEffect(stack);
        if (passiveEffect == null) {
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_passive").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.passive_hint").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.passive.name", passiveEffect.displayName()));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.passive.desc", passiveEffect.description()));
        lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.passive.value", passiveEffect.formatValue()));
        return lines;
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

    @Nullable
    public static EquipmentActiveSkill getActiveSkill(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(ACTIVE_SKILL_TAG)) {
            return null;
        }

        return EquipmentActiveSkill.byId(tag.getString(ACTIVE_SKILL_TAG));
    }

    @Nullable
    public static EquipmentPassiveEffect getPassiveEffect(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(PASSIVE_EFFECT_TAG)) {
            return null;
        }

        return EquipmentPassiveEffect.byId(tag.getString(PASSIVE_EFFECT_TAG));
    }

    private static void setQuality(ItemStack stack, EquipmentQuality quality) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null) {
            tag = new CompoundTag();
        }

        tag.putString(QUALITY_TAG, quality.id());
    writeDerivedDetails(tag, stack, quality);
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

    private static Component getEquipmentTypeLabel(ItemStack stack) {
        String suffix;
        if (stack.getItem() instanceof ArmorItem) {
            suffix = "armor";
        } else if (stack.getItem() instanceof TieredItem) {
            suffix = "weapon";
        } else {
            suffix = "other";
        }

        return Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.type." + suffix);
    }

    private static void writeDerivedDetails(CompoundTag tag, ItemStack stack, EquipmentQuality quality) {
        EquipmentActiveSkill activeSkill = pickActiveSkill(stack, quality);
        EquipmentPassiveEffect passiveEffect = pickPassiveEffect(stack, quality);

        if (activeSkill != null) {
            tag.putString(ACTIVE_SKILL_TAG, activeSkill.id());
        } else {
            tag.remove(ACTIVE_SKILL_TAG);
        }

        if (passiveEffect != null) {
            tag.putString(PASSIVE_EFFECT_TAG, passiveEffect.id());
        } else {
            tag.remove(PASSIVE_EFFECT_TAG);
        }
    }

    @Nullable
    private static EquipmentActiveSkill pickActiveSkill(ItemStack stack, EquipmentQuality quality) {
        if (!(stack.getItem() instanceof TieredItem)) {
            return null;
        }

        return switch (quality) {
            case RARE -> EquipmentActiveSkill.ARC_SLASH;
            case EPIC -> EquipmentActiveSkill.GUARD_PULSE;
            case LEGENDARY -> EquipmentActiveSkill.SHOCK_BURST;
            default -> null;
        };
    }

    @Nullable
    private static EquipmentPassiveEffect pickPassiveEffect(ItemStack stack, EquipmentQuality quality) {
        if (!(stack.getItem() instanceof TieredItem)) {
            return null;
        }

        return switch (quality) {
            case NORMAL -> EquipmentPassiveEffect.STEADY_EDGE;
            case UNCOMMON -> EquipmentPassiveEffect.SWIFT_STRIKE;
            case RARE -> EquipmentPassiveEffect.STEADY_EDGE;
            case EPIC -> EquipmentPassiveEffect.TITAN_GRIP;
            case LEGENDARY -> EquipmentPassiveEffect.GUARD_BREAKER;
            default -> null;
        };
    }

    private static Component formatAttributeLine(Attribute attribute, AttributeModifier modifier) {
        MutableComponent label = Component.translatable(attribute.getDescriptionId()).withStyle(ChatFormatting.GRAY);
        MutableComponent amount = Component.literal(formatModifierAmount(modifier)).withStyle(modifier.amount() >= 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED);
        return Component.empty().append(label).append(Component.literal(": ")).append(amount);
    }

    private static String formatModifierAmount(AttributeModifier modifier) {
        double amount = modifier.amount();
        return switch (modifier.operation()) {
            case ADD_VALUE -> formatSignedDecimal(amount);
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> formatSignedDecimal(amount * 100.0D) + "%";
        };
    }

    private static String formatSignedDecimal(double value) {
        String sign = value >= 0.0D ? "+" : "";
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return sign + formatted;
    }

    private static String formatCooldownSeconds(int cooldownTicks) {
        return String.format(Locale.ROOT, "%.1fs", cooldownTicks / 20.0D);
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