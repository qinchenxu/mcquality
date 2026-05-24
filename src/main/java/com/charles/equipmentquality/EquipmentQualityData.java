package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EquipmentQualityData {
    private static final String DATA_ROOT_TAG = "EquipmentQuality";
    private static final String LEGACY_ACTIVE_SKILL_TAG = "EquipmentQualityActiveSkill";
    private static final String LEGACY_PASSIVE_EFFECT_TAG = "EquipmentQualityPassiveEffect";
    private static final int DATA_VERSION = 2;

    private static final String VERSION_TAG = "version";
    private static final String RARITY_TAG = "rarity";
    private static final String AFFIXES_TAG = "affixes";
    private static final String ACTIVE_SKILL_DATA_TAG = "active_skill";
    private static final String PASSIVE_EFFECTS_TAG = "passive_effects";
    private static final String ID_TAG = "id";
    private static final String TRIGGER_TAG = "trigger";
    private static final String COOLDOWN_TICKS_TAG = "cooldown_ticks";
    private static final String PARTICLE_STYLE_TAG = "particle_style";
    private static final String PRIMARY_VALUE_TAG = "primary_value";
    private static final String VALUE_TAG = "value";
    private static final String UNIT_TAG = "unit";

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
                setQuality(stack, quality, random);
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
        if (isStructuredData(stack)) {
            tooltip.add(Component.translatable(
                "tooltip." + EquipmentQualityMod.MOD_ID + ".affix_count",
                Component.literal(Integer.toString(getAffixes(stack).size())).withStyle(quality.color())
            ).withStyle(ChatFormatting.DARK_GRAY));

            EquipmentActiveSkill activeSkill = getActiveSkill(stack);
            if (activeSkill != null) {
                tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".active_skill", activeSkill.displayName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".bonus", Component.literal(quality.signedPercent()).withStyle(quality.color())).withStyle(ChatFormatting.DARK_GRAY));
        }

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
        List<EquipmentAffixInstance> affixes = getAffixes(stack);

        List<Component> summaryLines = new ArrayList<>();
        summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.rarity", quality != null ? quality.displayName() : Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.none").withStyle(ChatFormatting.GRAY)));
        summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.type", getEquipmentTypeLabel(stack)));
        if (isStructuredData(stack)) {
            summaryLines.add(Component.translatable(
                "screen." + EquipmentQualityMod.MOD_ID + ".details.affix_count",
                Component.literal(Integer.toString(affixes.size())).withStyle(quality != null ? quality.color() : ChatFormatting.GRAY)
            ));
        } else {
            summaryLines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.attr", quality != null ? Component.literal(quality.signedPercent()).withStyle(quality.color()) : Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.attr.none").withStyle(ChatFormatting.GRAY)));
        }
        sections.add(new DetailSection(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.summary"), summaryLines));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.attributes"),
            getAttributeLines(stack)
        ));

        sections.add(new DetailSection(
            Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.section.affixes"),
            getAffixLines(stack)
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
        for (ResolvedAttributeEntry entry : collectEffectiveModifiers(stack).entries()) {
            lines.add(formatAttributeLine(entry.attribute().value(), entry.modifier()));
        }

        if (lines.isEmpty()) {
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_attributes").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static List<Component> getAffixLines(ItemStack stack) {
        List<EquipmentAffixInstance> affixes = getAffixes(stack);
        if (affixes.isEmpty()) {
            return List.of(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_affixes").withStyle(ChatFormatting.GRAY));
        }

        List<Component> lines = new ArrayList<>();
        for (EquipmentAffixInstance affix : affixes) {
            EquipmentAffixDefinition definition = EquipmentAffixDefinition.byId(affix.id());
            if (definition == null) {
                continue;
            }

            lines.add(definition.buildDisplayLine(affix));
        }

        if (lines.isEmpty()) {
            lines.add(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.no_affixes").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static List<Component> getActiveSkillLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        StoredActiveSkill skill = getActiveSkillData(stack);
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
        CompoundTag dataRoot = getDataRoot(stack);
        if (dataRoot != null && dataRoot.contains(RARITY_TAG, Tag.TAG_STRING)) {
            return EquipmentQuality.byId(dataRoot.getString(RARITY_TAG));
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(DATA_ROOT_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return EquipmentQuality.byId(tag.getString(DATA_ROOT_TAG));
    }

    @Nullable
    public static EquipmentActiveSkill getActiveSkill(ItemStack stack) {
        StoredActiveSkill activeSkill = getActiveSkillData(stack);
        return activeSkill != null ? activeSkill.template() : null;
    }

    @Nullable
    public static StoredActiveSkill getActiveSkillData(ItemStack stack) {
        CompoundTag dataRoot = getDataRoot(stack);
        if (dataRoot != null && dataRoot.contains(ACTIVE_SKILL_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag activeSkillTag = dataRoot.getCompound(ACTIVE_SKILL_DATA_TAG);
            if (activeSkillTag.contains(ID_TAG, Tag.TAG_STRING)) {
                String skillId = activeSkillTag.getString(ID_TAG);
                EquipmentSkillDefinition definition = EquipmentSkillDefinition.byId(skillId);
                EquipmentActiveSkill template = EquipmentActiveSkill.byId(skillId);

                return new StoredActiveSkill(
                    skillId,
                    activeSkillTag.contains(TRIGGER_TAG, Tag.TAG_STRING)
                        ? activeSkillTag.getString(TRIGGER_TAG)
                        : definition != null ? definition.triggerId() : template != null ? template.triggerId() : "right_click",
                    activeSkillTag.contains(COOLDOWN_TICKS_TAG, Tag.TAG_INT)
                        ? activeSkillTag.getInt(COOLDOWN_TICKS_TAG)
                        : definition != null ? definition.cooldownTicks() : template != null ? template.cooldownTicks() : 0,
                    activeSkillTag.contains(PARTICLE_STYLE_TAG, Tag.TAG_STRING)
                        ? activeSkillTag.getString(PARTICLE_STYLE_TAG)
                        : definition != null ? definition.particleStyleId() : template != null ? template.particleStyleId() : skillId,
                    activeSkillTag.contains(PRIMARY_VALUE_TAG, Tag.TAG_DOUBLE)
                        ? activeSkillTag.getDouble(PRIMARY_VALUE_TAG)
                        : definition != null ? definition.primaryValue() : template != null ? template.primaryValue() : 0.0D
                );
            }
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(LEGACY_ACTIVE_SKILL_TAG, Tag.TAG_STRING)) {
            return null;
        }

        EquipmentActiveSkill legacySkill = EquipmentActiveSkill.byId(tag.getString(LEGACY_ACTIVE_SKILL_TAG));
        return legacySkill != null ? StoredActiveSkill.fromLegacy(legacySkill) : null;
    }

    @Nullable
    public static EquipmentPassiveEffect getPassiveEffect(ItemStack stack) {
        CompoundTag dataRoot = getDataRoot(stack);
        if (dataRoot != null && dataRoot.contains(PASSIVE_EFFECTS_TAG, Tag.TAG_LIST)) {
            ListTag passiveEffects = dataRoot.getList(PASSIVE_EFFECTS_TAG, Tag.TAG_COMPOUND);
            if (!passiveEffects.isEmpty()) {
                CompoundTag passiveEffectTag = passiveEffects.getCompound(0);
                if (passiveEffectTag.contains(ID_TAG, Tag.TAG_STRING)) {
                    return EquipmentPassiveEffect.byId(passiveEffectTag.getString(ID_TAG));
                }
            }
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(LEGACY_PASSIVE_EFFECT_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return EquipmentPassiveEffect.byId(tag.getString(LEGACY_PASSIVE_EFFECT_TAG));
    }

    public static List<EquipmentAffixInstance> getAffixes(ItemStack stack) {
        CompoundTag dataRoot = getDataRoot(stack);
        if (dataRoot == null || !dataRoot.contains(AFFIXES_TAG, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag affixTags = dataRoot.getList(AFFIXES_TAG, Tag.TAG_COMPOUND);
        List<EquipmentAffixInstance> affixes = new ArrayList<>();
        for (int index = 0; index < affixTags.size(); index++) {
            EquipmentAffixInstance affix = EquipmentAffixInstance.fromTag(affixTags.getCompound(index));
            if (affix != null) {
                affixes.add(affix);
            }
        }

        return affixes;
    }

    private static void setQuality(ItemStack stack, EquipmentQuality quality, @Nullable RandomSource random) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null) {
            tag = new CompoundTag();
        }

        CompoundTag dataRoot = getOrCreateDataRoot(tag);
        dataRoot.putString(RARITY_TAG, quality.id());
        List<EquipmentAffixInstance> affixes = random != null ? EquipmentAffixDefinition.rollAffixes(stack, quality, random) : List.of();
        writeAffixes(dataRoot, affixes);

        GeneratedDetails details = writeDerivedDetails(dataRoot, stack, quality, random);
        tag.remove(LEGACY_ACTIVE_SKILL_TAG);
        tag.remove(LEGACY_PASSIVE_EFFECT_TAG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (random != null) {
            applyAffixModifiers(stack, affixes);
        } else {
            applyQualityModifiers(stack, quality);
        }
        applyQualityLore(stack, quality, affixes.size(), details.activeSkill());
    }

    public static void copyQuality(ItemStack source, ItemStack target) {
        EquipmentQuality quality = getQuality(source);
        if (quality == null || !isSupported(target) || getQuality(target) != null) {
            return;
        }

        CompoundTag sourceDataRoot = getDataRoot(source);
        if (sourceDataRoot != null) {
            CustomData customData = target.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag targetTag = customData.copyTag();
            if (targetTag == null) {
                targetTag = new CompoundTag();
            }

            targetTag.put(DATA_ROOT_TAG, sourceDataRoot.copy());
            targetTag.remove(LEGACY_ACTIVE_SKILL_TAG);
            targetTag.remove(LEGACY_PASSIVE_EFFECT_TAG);
            target.set(DataComponents.CUSTOM_DATA, CustomData.of(targetTag));
            target.set(DataComponents.ATTRIBUTE_MODIFIERS, source.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));

            ItemLore sourceLore = source.get(DataComponents.LORE);
            if (sourceLore != null) {
                target.set(DataComponents.LORE, sourceLore);
            }
            return;
        }

        setQuality(target, quality, null);
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

    private static GeneratedDetails writeDerivedDetails(CompoundTag dataRoot, ItemStack stack, EquipmentQuality quality, @Nullable RandomSource random) {
        EquipmentSkillDefinition activeSkill = pickActiveSkill(stack, quality, random);
        EquipmentPassiveEffect passiveEffect = pickPassiveEffect(stack, quality, random);

        if (activeSkill != null) {
            CompoundTag activeSkillTag = new CompoundTag();
            activeSkillTag.putString(ID_TAG, activeSkill.id());
            activeSkillTag.putString(TRIGGER_TAG, activeSkill.triggerId());
            activeSkillTag.putInt(COOLDOWN_TICKS_TAG, activeSkill.cooldownTicks());
            activeSkillTag.putString(PARTICLE_STYLE_TAG, activeSkill.particleStyleId());
            activeSkillTag.putDouble(PRIMARY_VALUE_TAG, activeSkill.primaryValue());
            dataRoot.put(ACTIVE_SKILL_DATA_TAG, activeSkillTag);
        } else {
            dataRoot.remove(ACTIVE_SKILL_DATA_TAG);
        }

        ListTag passiveEffects = new ListTag();
        if (passiveEffect != null) {
            CompoundTag passiveEffectTag = new CompoundTag();
            passiveEffectTag.putString(ID_TAG, passiveEffect.id());
            passiveEffectTag.putDouble(VALUE_TAG, passiveEffect.value());
            passiveEffectTag.putString(UNIT_TAG, passiveEffect.percent() ? "percent" : "flat");
            passiveEffects.add(passiveEffectTag);
        }

        dataRoot.put(PASSIVE_EFFECTS_TAG, passiveEffects);
        return new GeneratedDetails(activeSkill, passiveEffect);
    }

    @Nullable
    private static CompoundTag getDataRoot(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag == null || !tag.contains(DATA_ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        return tag.getCompound(DATA_ROOT_TAG);
    }

    private static CompoundTag getOrCreateDataRoot(CompoundTag tag) {
        CompoundTag dataRoot;
        if (tag.contains(DATA_ROOT_TAG, Tag.TAG_COMPOUND)) {
            dataRoot = tag.getCompound(DATA_ROOT_TAG);
        } else {
            dataRoot = new CompoundTag();
            tag.put(DATA_ROOT_TAG, dataRoot);
        }

        dataRoot.putInt(VERSION_TAG, DATA_VERSION);
        return dataRoot;
    }

    @Nullable
    private static EquipmentSkillDefinition pickActiveSkill(ItemStack stack, EquipmentQuality quality, @Nullable RandomSource random) {
        if (!(stack.getItem() instanceof TieredItem)) {
            return null;
        }

        if (random == null) {
            return switch (quality) {
                case RARE -> EquipmentSkillDefinition.byId(EquipmentActiveSkill.ARC_SLASH.id());
                case EPIC -> EquipmentSkillDefinition.byId(EquipmentActiveSkill.GUARD_PULSE.id());
                case LEGENDARY -> EquipmentSkillDefinition.byId(EquipmentActiveSkill.SHOCK_BURST.id());
                default -> null;
            };
        }

        return EquipmentSkillDefinition.randomFor(stack, quality, random);
    }

    @Nullable
    private static EquipmentPassiveEffect pickPassiveEffect(ItemStack stack, EquipmentQuality quality, @Nullable RandomSource random) {
        if (!(stack.getItem() instanceof TieredItem)) {
            return null;
        }

        if (random == null) {
            return switch (quality) {
                case NORMAL -> EquipmentPassiveEffect.STEADY_EDGE;
                case UNCOMMON -> EquipmentPassiveEffect.SWIFT_STRIKE;
                case RARE -> EquipmentPassiveEffect.STEADY_EDGE;
                case EPIC -> EquipmentPassiveEffect.TITAN_GRIP;
                case LEGENDARY -> EquipmentPassiveEffect.GUARD_BREAKER;
                default -> null;
            };
        }

        List<EquipmentPassiveEffect> candidates = new ArrayList<>();
        if (quality.displayPriority() >= EquipmentQuality.NORMAL.displayPriority()) {
            candidates.add(EquipmentPassiveEffect.STEADY_EDGE);
        }
        if (quality.displayPriority() >= EquipmentQuality.UNCOMMON.displayPriority()) {
            candidates.add(EquipmentPassiveEffect.SWIFT_STRIKE);
        }
        if (quality.displayPriority() >= EquipmentQuality.RARE.displayPriority()) {
            candidates.add(EquipmentPassiveEffect.TITAN_GRIP);
        }
        if (quality.displayPriority() >= EquipmentQuality.EPIC.displayPriority()) {
            candidates.add(EquipmentPassiveEffect.GUARD_BREAKER);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(random.nextInt(candidates.size()));
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

    private static void writeAffixes(CompoundTag dataRoot, List<EquipmentAffixInstance> affixes) {
        ListTag affixTags = new ListTag();
        for (EquipmentAffixInstance affix : affixes) {
            affixTags.add(affix.toTag());
        }

        dataRoot.put(AFFIXES_TAG, affixTags);
    }

    private static boolean isStructuredData(ItemStack stack) {
        return getDataRoot(stack) != null;
    }

    private static void applyAffixModifiers(ItemStack stack, List<EquipmentAffixInstance> affixes) {
        CollectedModifiers collectedModifiers = collectEffectiveModifiers(stack);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ResolvedAttributeEntry entry : collectedModifiers.entries()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slotGroup());
        }

        for (EquipmentAffixInstance affix : affixes) {
            EquipmentAffixDefinition definition = EquipmentAffixDefinition.byId(affix.id());
            if (definition == null) {
                continue;
            }

            double resolvedAmount = resolveAffixAmount(definition, affix, collectedModifiers.additiveBaseAmounts());
            if (Double.compare(resolvedAmount, 0.0D) == 0) {
                continue;
            }

            builder.add(definition.attribute(), definition.createResolvedModifier(resolvedAmount), collectedModifiers.slotGroup());
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build().withTooltip(collectedModifiers.showInTooltip()));
    }

    private static void applyQualityModifiers(ItemStack stack, EquipmentQuality quality) {
        CollectedModifiers collectedModifiers = collectEffectiveModifiers(stack);
        double factor = 1.0D + quality.multiplierBonus();
        if (factor <= 0.0D) {
            return;
        }

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        boolean changed = false;
        for (ResolvedAttributeEntry entry : collectedModifiers.entries()) {
            AttributeModifier scaledModifier = scaleModifier(entry.attribute().value(), entry.modifier(), factor);
            builder.add(entry.attribute(), scaledModifier != null ? scaledModifier : entry.modifier(), entry.slotGroup());
            changed |= scaledModifier != null;
        }

        if (changed) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build().withTooltip(collectedModifiers.showInTooltip()));
        }
    }

    private static double resolveAffixAmount(
        EquipmentAffixDefinition definition,
        EquipmentAffixInstance affix,
        Map<Holder<Attribute>, Double> additiveBaseAmounts
    ) {
        if (!definition.usesPercentValue()) {
            return affix.value();
        }

        double baseAmount = additiveBaseAmounts.getOrDefault(definition.attribute(), 0.0D);
        return baseAmount * (affix.value() / 100.0D);
    }

    private static CollectedModifiers collectEffectiveModifiers(ItemStack stack) {
        EquipmentSlotGroup slotGroup = EquipmentAffixDefinition.resolveSlotGroup(stack);
        boolean showInTooltip = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip();
        List<ResolvedAttributeEntry> entries = new ArrayList<>();
        Map<Holder<Attribute>, Double> additiveBaseAmounts = new LinkedHashMap<>();
        stack.forEachModifier(slotGroup, (attribute, modifier) -> {
            entries.add(new ResolvedAttributeEntry(attribute, modifier, slotGroup));
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                additiveBaseAmounts.merge(attribute, modifier.amount(), Double::sum);
            }
        });
        return new CollectedModifiers(slotGroup, showInTooltip, entries, additiveBaseAmounts);
    }

    private static void applyQualityLore(ItemStack stack, EquipmentQuality quality, int affixCount, @Nullable EquipmentSkillDefinition activeSkill) {
        List<Component> loreLines = new ArrayList<>();
        loreLines.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".quality", quality.displayName()).withStyle(ChatFormatting.DARK_GRAY));
        loreLines.add(Component.translatable(
            "tooltip." + EquipmentQualityMod.MOD_ID + ".affix_count",
            Component.literal(Integer.toString(affixCount)).withStyle(quality.color())
        ).withStyle(ChatFormatting.DARK_GRAY));

        if (activeSkill != null) {
            loreLines.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".active_skill", activeSkill.displayName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (supportsDetailsPanel(stack)) {
            loreLines.add(Component.translatable("tooltip." + EquipmentQualityMod.MOD_ID + ".details_hint").withStyle(ChatFormatting.DARK_GRAY));
        }

        stack.set(DataComponents.LORE, new ItemLore(loreLines));
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

    public record StoredActiveSkill(String id, String triggerId, int cooldownTicks, String particleStyleId, double primaryValue) {
        @Nullable
        public EquipmentActiveSkill template() {
            return EquipmentActiveSkill.byId(id);
        }

        public Component displayName() {
            EquipmentSkillDefinition definition = EquipmentSkillDefinition.byId(id);
            if (definition != null) {
                return definition.displayName();
            }

            EquipmentActiveSkill skill = template();
            return skill != null ? skill.displayName() : Component.literal(id);
        }

        public Component description() {
            EquipmentSkillDefinition definition = EquipmentSkillDefinition.byId(id);
            if (definition != null) {
                return Component.translatable(definition.descKey(), definition.formatPrimaryValue(primaryValue));
            }

            return Component.translatable("skill_desc." + EquipmentQualityMod.MOD_ID + "." + id, formatPrimaryValue());
        }

        public Component triggerName() {
            return Component.translatable("skill_trigger." + EquipmentQualityMod.MOD_ID + "." + triggerId);
        }

        public Component particleStyleName() {
            return Component.translatable("particle_style." + EquipmentQualityMod.MOD_ID + "." + particleStyleId);
        }

        public String formatPrimaryValue() {
            EquipmentSkillDefinition definition = EquipmentSkillDefinition.byId(id);
            if (definition != null) {
                return definition.formatPrimaryValue(primaryValue);
            }

            EquipmentActiveSkill skill = template();
            if (skill == EquipmentActiveSkill.ARC_SLASH || skill == EquipmentActiveSkill.SHOCK_BURST) {
                return String.format(java.util.Locale.ROOT, "%.1f%%", primaryValue * 100.0D);
            }
            return String.format(java.util.Locale.ROOT, "%.1f", primaryValue);
        }

        public static StoredActiveSkill fromLegacy(EquipmentActiveSkill skill) {
            return new StoredActiveSkill(skill.id(), skill.triggerId(), skill.cooldownTicks(), skill.particleStyleId(), skill.primaryValue());
        }
    }

    private record ResolvedAttributeEntry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slotGroup) {
    }

    private record CollectedModifiers(
        EquipmentSlotGroup slotGroup,
        boolean showInTooltip,
        List<ResolvedAttributeEntry> entries,
        Map<Holder<Attribute>, Double> additiveBaseAmounts
    ) {
    }

    private record GeneratedDetails(@Nullable EquipmentSkillDefinition activeSkill, @Nullable EquipmentPassiveEffect passiveEffect) {
    }
}