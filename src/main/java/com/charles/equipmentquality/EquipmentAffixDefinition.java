package com.charles.equipmentquality;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record EquipmentAffixDefinition(
    String id,
    EquipmentKind kind,
    String category,
    double minValue,
    double maxValue,
    String unit,
    int displayOrder,
    int weight,
    Holder<Attribute> attribute,
    AttributeModifier.Operation operation
) {
    private static final List<EquipmentAffixDefinition> WEAPON_DEFINITIONS = List.of(
        new EquipmentAffixDefinition("weapon.attack_damage_percent", EquipmentKind.WEAPON, "offense", 4.0D, 18.0D, "percent", 10, 24, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("weapon.attack_speed_flat", EquipmentKind.WEAPON, "offense", 0.1D, 0.5D, "flat", 20, 16, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("weapon.attack_knockback_flat", EquipmentKind.WEAPON, "utility", 0.2D, 0.8D, "flat", 30, 10, Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE)
    );
    private static final List<EquipmentAffixDefinition> ARMOR_DEFINITIONS = List.of(
        new EquipmentAffixDefinition("armor.armor_percent", EquipmentKind.ARMOR, "defense", 4.0D, 16.0D, "percent", 10, 24, Attributes.ARMOR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("armor.armor_toughness_flat", EquipmentKind.ARMOR, "defense", 0.5D, 3.0D, "flat", 20, 14, Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("armor.knockback_resistance_flat", EquipmentKind.ARMOR, "defense", 0.03D, 0.12D, "flat", 30, 8, Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE)
    );
    private static final List<EquipmentAffixDefinition> TOOL_DEFINITIONS = List.of(
        new EquipmentAffixDefinition("tool.mining_speed_percent", EquipmentKind.TOOL, "utility", 6.0D, 20.0D, "percent", 10, 24, Attributes.MINING_EFFICIENCY, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("tool.attack_damage_flat", EquipmentKind.TOOL, "offense", 0.5D, 2.5D, "flat", 20, 10, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("tool.block_break_speed_percent", EquipmentKind.TOOL, "utility", 6.0D, 18.0D, "percent", 30, 18, Attributes.BLOCK_BREAK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );
    private static final List<EquipmentAffixDefinition> ALL_DEFINITIONS = List.of(
        WEAPON_DEFINITIONS.get(0), WEAPON_DEFINITIONS.get(1), WEAPON_DEFINITIONS.get(2),
        ARMOR_DEFINITIONS.get(0), ARMOR_DEFINITIONS.get(1), ARMOR_DEFINITIONS.get(2),
        TOOL_DEFINITIONS.get(0), TOOL_DEFINITIONS.get(1), TOOL_DEFINITIONS.get(2)
    );

    public static List<EquipmentAffixInstance> rollAffixes(ItemStack stack, EquipmentQuality quality, RandomSource random) {
        List<EquipmentAffixDefinition> pool = new ArrayList<>(poolFor(stack));
        if (pool.isEmpty()) {
            return List.of();
        }

        int affixRange = Math.max(0, quality.maxAffixCount() - quality.minAffixCount());
        int requestedCount = quality.minAffixCount() + (affixRange == 0 ? 0 : random.nextInt(affixRange + 1));
        requestedCount = Math.min(requestedCount, pool.size());
        if (requestedCount <= 0) {
            return List.of();
        }

        List<EquipmentAffixInstance> generated = new ArrayList<>();
        for (int index = 0; index < requestedCount; index++) {
            EquipmentAffixDefinition definition = pickRandomDefinition(pool, random);
            if (definition == null) {
                break;
            }

            pool.remove(definition);
            generated.add(new EquipmentAffixInstance(
                definition.id(),
                quality.id(),
                rollValue(definition, random),
                definition.unit(),
                definition.displayOrder(),
                definition.category()
            ));
        }

        generated.sort(Comparator.comparingInt(EquipmentAffixInstance::displayOrder));
        return generated;
    }

    @Nullable
    public static EquipmentAffixDefinition byId(String id) {
        for (EquipmentAffixDefinition definition : ALL_DEFINITIONS) {
            if (definition.id.equals(id)) {
                return definition;
            }
        }
        return null;
    }

    public static EquipmentSlotGroup resolveSlotGroup(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return switch (armorItem.getEquipmentSlot()) {
                case HEAD -> EquipmentSlotGroup.HEAD;
                case CHEST -> EquipmentSlotGroup.CHEST;
                case LEGS -> EquipmentSlotGroup.LEGS;
                case FEET -> EquipmentSlotGroup.FEET;
                case BODY -> EquipmentSlotGroup.BODY;
                default -> EquipmentSlotGroup.ANY;
            };
        }

        return EquipmentSlotGroup.MAINHAND;
    }

    public AttributeModifier createModifier(EquipmentAffixInstance instance) {
        double amount = "percent".equals(unit) ? instance.value() / 100.0D : instance.value();
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
            EquipmentQualityMod.MOD_ID,
            "affix_" + id.replace('.', '_')
        );
        return new AttributeModifier(modifierId, amount, operation);
    }

    public Component buildDisplayLine(EquipmentAffixInstance instance) {
        return Component.empty()
            .append(Component.translatable(translationKey()).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(": "))
            .append(Component.literal(formatValue(instance.value())).withStyle(instance.value() >= 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    public String translationKey() {
        return "affix." + EquipmentQualityMod.MOD_ID + "." + id;
    }

    private String formatValue(double value) {
        if ("percent".equals(unit)) {
            return formatSignedDecimal(value) + "%";
        }
        return formatSignedDecimal(value);
    }

    private static List<EquipmentAffixDefinition> poolFor(ItemStack stack) {
        EquipmentKind kind = resolveKind(stack);
        if (kind == null) {
            return List.of();
        }

        return switch (kind) {
            case WEAPON -> WEAPON_DEFINITIONS;
            case ARMOR -> ARMOR_DEFINITIONS;
            case TOOL -> TOOL_DEFINITIONS;
        };
    }

    @Nullable
    private static EquipmentAffixDefinition pickRandomDefinition(List<EquipmentAffixDefinition> definitions, RandomSource random) {
        if (definitions.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (EquipmentAffixDefinition definition : definitions) {
            totalWeight += definition.weight();
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (EquipmentAffixDefinition definition : definitions) {
            current += definition.weight();
            if (roll < current) {
                return definition;
            }
        }

        return definitions.get(definitions.size() - 1);
    }

    private static double rollValue(EquipmentAffixDefinition definition, RandomSource random) {
        double rawValue = definition.minValue() + (random.nextDouble() * (definition.maxValue() - definition.minValue()));
        return Math.round(rawValue * 10.0D) / 10.0D;
    }

    @Nullable
    private static EquipmentKind resolveKind(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem) {
            return EquipmentKind.ARMOR;
        }
        if (stack.getItem() instanceof DiggerItem) {
            return EquipmentKind.TOOL;
        }
        if (stack.getItem() instanceof TieredItem) {
            return EquipmentKind.WEAPON;
        }
        return null;
    }

    private static String formatSignedDecimal(double value) {
        String sign = value >= 0.0D ? "+" : "";
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return sign + formatted;
    }

    public enum EquipmentKind {
        WEAPON,
        ARMOR,
        TOOL
    }
}