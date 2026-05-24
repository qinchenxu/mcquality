package com.charles.equipmentquality;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record EquipmentAffixDefinition(
    String id,
    Set<EquipmentKind> kinds,
    String category,
    double minValue,
    double maxValue,
    int precision,
    String unit,
    int displayOrder,
    int weight,
    Holder<Attribute> attribute,
    AttributeModifier.Operation operation
) {
    private static final List<EquipmentAffixDefinition> FALLBACK_DEFINITIONS = List.of(
        new EquipmentAffixDefinition("weapon.attack_damage_percent", Set.of(EquipmentKind.WEAPON), "offense", 4.0D, 18.0D, 1, "percent", 10, 24, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("weapon.attack_speed_flat", Set.of(EquipmentKind.WEAPON), "offense", 0.1D, 0.5D, 1, "flat", 20, 16, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("weapon.attack_knockback_flat", Set.of(EquipmentKind.WEAPON), "utility", 0.2D, 0.8D, 1, "flat", 30, 10, Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("armor.armor_percent", Set.of(EquipmentKind.ARMOR), "defense", 4.0D, 16.0D, 1, "percent", 10, 24, Attributes.ARMOR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("armor.armor_toughness_flat", Set.of(EquipmentKind.ARMOR), "defense", 0.5D, 3.0D, 1, "flat", 20, 14, Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("armor.knockback_resistance_flat", Set.of(EquipmentKind.ARMOR), "defense", 0.03D, 0.12D, 2, "flat", 30, 8, Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("tool.mining_speed_percent", Set.of(EquipmentKind.TOOL), "utility", 6.0D, 20.0D, 1, "percent", 10, 24, Attributes.MINING_EFFICIENCY, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        new EquipmentAffixDefinition("tool.attack_damage_flat", Set.of(EquipmentKind.TOOL), "offense", 0.5D, 2.5D, 1, "flat", 20, 10, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
        new EquipmentAffixDefinition("tool.block_break_speed_percent", Set.of(EquipmentKind.TOOL), "utility", 6.0D, 18.0D, 1, "percent", 30, 18, Attributes.BLOCK_BREAK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );
    private static volatile Map<String, EquipmentAffixDefinition> loadedDefinitions = Map.of();

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
        Map<String, EquipmentAffixDefinition> definitionsById = activeDefinitionsById();
        return definitionsById.get(id);
    }

    public static void replaceLoadedDefinitions(List<EquipmentAffixDefinition> definitions) {
        if (definitions.isEmpty()) {
            loadedDefinitions = Map.of();
            return;
        }

        Map<String, EquipmentAffixDefinition> definitionsById = new LinkedHashMap<>();
        for (EquipmentAffixDefinition definition : definitions) {
            definitionsById.put(definition.id(), definition);
        }
        loadedDefinitions = Map.copyOf(definitionsById);
    }

    @Nullable
    public static EquipmentAffixDefinition fromJson(JsonObject json) {
        if (!json.has("id") || !json.has("value") || !json.has("attribute_effect")) {
            return null;
        }

        JsonObject valueObject = json.getAsJsonObject("value");
        JsonObject attributeEffectObject = json.getAsJsonObject("attribute_effect");
        Set<EquipmentKind> parsedKinds = parseKinds(json);
        Holder<Attribute> parsedAttribute = resolveAttribute(requiredString(attributeEffectObject, "target"));
        if (parsedKinds.isEmpty() || parsedAttribute == null) {
            return null;
        }

        return new EquipmentAffixDefinition(
            requiredString(json, "id"),
            parsedKinds,
            optionalString(json, "category", "utility"),
            requiredDouble(valueObject, "min"),
            requiredDouble(valueObject, "max"),
            optionalInt(valueObject, "precision", 1),
            optionalString(valueObject, "unit", "flat"),
            optionalInt(json, "display_order", 100),
            optionalInt(json, "weight", 10),
            parsedAttribute,
            resolveOperation(optionalString(attributeEffectObject, "mode", "add_flat"))
        );
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

    public boolean usesPercentValue() {
        return "percent".equals(unit);
    }

    public AttributeModifier createModifier(EquipmentAffixInstance instance) {
        double amount = usesPercentValue() ? instance.value() / 100.0D : instance.value();
        return createModifier(amount, operation);
    }

    public AttributeModifier createResolvedModifier(double amount) {
        return createModifier(amount, usesPercentValue() ? AttributeModifier.Operation.ADD_VALUE : operation);
    }

    private AttributeModifier createModifier(double amount, AttributeModifier.Operation resolvedOperation) {
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
            EquipmentQualityMod.MOD_ID,
            "affix_" + id.replace('.', '_')
        );
        return new AttributeModifier(modifierId, amount, resolvedOperation);
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

        List<EquipmentAffixDefinition> matchingDefinitions = new ArrayList<>();
        for (EquipmentAffixDefinition definition : activeDefinitions()) {
            if (definition.kinds().contains(kind)) {
                matchingDefinitions.add(definition);
            }
        }
        return matchingDefinitions;
    }

    private static Map<String, EquipmentAffixDefinition> activeDefinitionsById() {
        if (!loadedDefinitions.isEmpty()) {
            return loadedDefinitions;
        }

        Map<String, EquipmentAffixDefinition> fallbackDefinitions = new LinkedHashMap<>();
        for (EquipmentAffixDefinition definition : FALLBACK_DEFINITIONS) {
            fallbackDefinitions.put(definition.id(), definition);
        }
        return fallbackDefinitions;
    }

    private static List<EquipmentAffixDefinition> activeDefinitions() {
        return new ArrayList<>(activeDefinitionsById().values());
    }

    private static double rollValue(EquipmentAffixDefinition definition, RandomSource random) {
        double rawValue = definition.minValue() + (random.nextDouble() * (definition.maxValue() - definition.minValue()));
        double scale = Math.pow(10.0D, Math.max(0, definition.precision()));
        return Math.round(rawValue * scale) / scale;
    }

    @Nullable
    public static EquipmentKind resolveKind(ItemStack stack) {
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

    private static Set<EquipmentKind> parseKinds(JsonObject json) {
        if (json.has("equipment_types") && json.get("equipment_types").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("equipment_types");
            java.util.LinkedHashSet<EquipmentKind> kinds = new java.util.LinkedHashSet<>();
            for (JsonElement element : array) {
                EquipmentKind kind = EquipmentKind.byId(element.getAsString());
                if (kind != null) {
                    kinds.add(kind);
                }
            }
            return Set.copyOf(kinds);
        }

        if (json.has("equipment_type")) {
            EquipmentKind kind = EquipmentKind.byId(json.get("equipment_type").getAsString());
            if (kind != null) {
                return Set.of(kind);
            }
        };

        return Set.of();
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

    @Nullable
    private static Holder<Attribute> resolveAttribute(String target) {
        return switch (target) {
            case "minecraft:generic.attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "minecraft:generic.attack_speed" -> Attributes.ATTACK_SPEED;
            case "minecraft:generic.attack_knockback" -> Attributes.ATTACK_KNOCKBACK;
            case "minecraft:generic.armor" -> Attributes.ARMOR;
            case "minecraft:generic.armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "minecraft:generic.knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
            case "minecraft:player.mining_efficiency" -> Attributes.MINING_EFFICIENCY;
            case "minecraft:player.block_break_speed" -> Attributes.BLOCK_BREAK_SPEED;
            default -> null;
        };
    }

    private static AttributeModifier.Operation resolveOperation(String mode) {
        return switch (mode) {
            case "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static String requiredString(JsonObject json, String key) {
        return json.get(key).getAsString();
    }

    private static String optionalString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static double requiredDouble(JsonObject json, String key) {
        return json.get(key).getAsDouble();
    }

    private static int optionalInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
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
        TOOL;

        @Nullable
        public static EquipmentKind byId(String id) {
            return switch (id) {
                case "weapon" -> WEAPON;
                case "armor" -> ARMOR;
                case "tool" -> TOOL;
                default -> null;
            };
        }
    }
}