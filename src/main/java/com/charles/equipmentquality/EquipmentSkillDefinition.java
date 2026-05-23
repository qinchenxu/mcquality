package com.charles.equipmentquality;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record EquipmentSkillDefinition(
    String id,
    Set<EquipmentAffixDefinition.EquipmentKind> kinds,
    String triggerId,
    int cooldownTicks,
    String particleStyleId,
    double primaryValue,
    String primaryValueFormat,
    int weight,
    EquipmentQuality minimumQuality,
    String nameKey,
    String descKey
) {
    private static final List<EquipmentSkillDefinition> FALLBACK_DEFINITIONS = List.of(
        new EquipmentSkillDefinition("arc_slash", Set.of(EquipmentAffixDefinition.EquipmentKind.WEAPON), "right_click", 80, "arc", 1.6D, "percent_multiplier", 8, EquipmentQuality.UNCOMMON, "skill." + EquipmentQualityMod.MOD_ID + ".arc_slash", "skill_desc." + EquipmentQualityMod.MOD_ID + ".arc_slash"),
        new EquipmentSkillDefinition("guard_pulse", Set.of(EquipmentAffixDefinition.EquipmentKind.WEAPON), "right_click", 120, "shield", 6.0D, "flat", 6, EquipmentQuality.RARE, "skill." + EquipmentQualityMod.MOD_ID + ".guard_pulse", "skill_desc." + EquipmentQualityMod.MOD_ID + ".guard_pulse"),
        new EquipmentSkillDefinition("shock_burst", Set.of(EquipmentAffixDefinition.EquipmentKind.WEAPON), "right_click", 100, "burst", 2.4D, "percent_multiplier", 4, EquipmentQuality.EPIC, "skill." + EquipmentQualityMod.MOD_ID + ".shock_burst", "skill_desc." + EquipmentQualityMod.MOD_ID + ".shock_burst")
    );
    private static volatile Map<String, EquipmentSkillDefinition> loadedDefinitions = Map.of();

    public static void replaceLoadedDefinitions(List<EquipmentSkillDefinition> definitions) {
        if (definitions.isEmpty()) {
            loadedDefinitions = Map.of();
            return;
        }

        Map<String, EquipmentSkillDefinition> definitionsById = new LinkedHashMap<>();
        for (EquipmentSkillDefinition definition : definitions) {
            definitionsById.put(definition.id(), definition);
        }
        loadedDefinitions = Map.copyOf(definitionsById);
    }

    @Nullable
    public static EquipmentSkillDefinition byId(String id) {
        return activeDefinitionsById().get(id);
    }

    @Nullable
    public static EquipmentSkillDefinition fromJson(JsonObject json) {
        if (!json.has("id")) {
            return null;
        }

        Set<EquipmentAffixDefinition.EquipmentKind> kinds = parseKinds(json);
        if (kinds.isEmpty()) {
            return null;
        }

        EquipmentQuality minimumQuality = EquipmentQuality.byId(optionalString(json, "minimum_rarity", EquipmentQuality.WORN.id()));
        if (minimumQuality == null) {
            minimumQuality = EquipmentQuality.WORN;
        }

        String id = requiredString(json, "id");
        return new EquipmentSkillDefinition(
            id,
            kinds,
            optionalString(json, "trigger", "right_click"),
            optionalInt(json, "cooldown_ticks", 80),
            optionalString(json, "particle_style", id),
            optionalDouble(json, "primary_value", 1.0D),
            optionalString(json, "primary_value_format", defaultValueFormat(id)),
            optionalInt(json, "weight", 10),
            minimumQuality,
            optionalString(json, "name_key", "skill." + EquipmentQualityMod.MOD_ID + "." + id),
            optionalString(json, "desc_key", "skill_desc." + EquipmentQualityMod.MOD_ID + "." + id)
        );
    }

    @Nullable
    public static EquipmentSkillDefinition randomFor(ItemStack stack, EquipmentQuality quality, RandomSource random) {
        EquipmentAffixDefinition.EquipmentKind kind = EquipmentAffixDefinition.resolveKind(stack);
        if (kind == null || random.nextDouble() > quality.skillChance()) {
            return null;
        }

        List<EquipmentSkillDefinition> candidates = new ArrayList<>();
        for (EquipmentSkillDefinition definition : activeDefinitions()) {
            if (definition.kinds().contains(kind) && quality.displayPriority() >= definition.minimumQuality().displayPriority()) {
                candidates.add(definition);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (EquipmentSkillDefinition definition : candidates) {
            totalWeight += definition.weight();
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (EquipmentSkillDefinition definition : candidates) {
            current += definition.weight();
            if (roll < current) {
                return definition;
            }
        }

        return candidates.get(candidates.size() - 1);
    }

    public Component displayName() {
        return Component.translatable(nameKey);
    }

    public Component description() {
        return Component.translatable(descKey, formatPrimaryValue(primaryValue));
    }

    public Component triggerName() {
        return Component.translatable("skill_trigger." + EquipmentQualityMod.MOD_ID + "." + triggerId);
    }

    public Component particleStyleName() {
        return Component.translatable("particle_style." + EquipmentQualityMod.MOD_ID + "." + particleStyleId);
    }

    public String formatPrimaryValue(double value) {
        return switch (primaryValueFormat) {
            case "percent" -> String.format(Locale.ROOT, "%.1f%%", value);
            case "percent_multiplier" -> String.format(Locale.ROOT, "%.1f%%", value * 100.0D);
            default -> String.format(Locale.ROOT, "%.1f", value);
        };
    }

    private static Map<String, EquipmentSkillDefinition> activeDefinitionsById() {
        if (!loadedDefinitions.isEmpty()) {
            return loadedDefinitions;
        }

        Map<String, EquipmentSkillDefinition> fallbackDefinitions = new LinkedHashMap<>();
        for (EquipmentSkillDefinition definition : FALLBACK_DEFINITIONS) {
            fallbackDefinitions.put(definition.id(), definition);
        }
        return fallbackDefinitions;
    }

    private static List<EquipmentSkillDefinition> activeDefinitions() {
        return new ArrayList<>(activeDefinitionsById().values());
    }

    private static Set<EquipmentAffixDefinition.EquipmentKind> parseKinds(JsonObject json) {
        if (json.has("equipment_types") && json.get("equipment_types").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("equipment_types");
            java.util.LinkedHashSet<EquipmentAffixDefinition.EquipmentKind> kinds = new java.util.LinkedHashSet<>();
            for (JsonElement element : array) {
                EquipmentAffixDefinition.EquipmentKind kind = EquipmentAffixDefinition.EquipmentKind.byId(element.getAsString());
                if (kind != null) {
                    kinds.add(kind);
                }
            }
            return Set.copyOf(kinds);
        }

        return Set.of(EquipmentAffixDefinition.EquipmentKind.WEAPON);
    }

    private static String defaultValueFormat(String id) {
        return switch (id) {
            case "arc_slash", "shock_burst" -> "percent_multiplier";
            default -> "flat";
        };
    }

    private static String requiredString(JsonObject json, String key) {
        return json.get(key).getAsString();
    }

    private static String optionalString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int optionalInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double optionalDouble(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}