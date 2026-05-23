package com.charles.equipmentquality;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DataDrivenDefinitionReloaders {
    private static final Gson GSON = new GsonBuilder().create();

    private DataDrivenDefinitionReloaders() {
    }

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AffixReloadListener());
        event.addListener(new SkillReloadListener());
    }

    private static final class AffixReloadListener extends SimpleJsonResourceReloadListener {
        private AffixReloadListener() {
            super(GSON, "affixes");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<EquipmentAffixDefinition> definitions = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    EquipmentQualityMod.LOGGER.warn("Skipping affix definition {} because it is not a JSON object", entry.getKey());
                    continue;
                }

                try {
                    EquipmentAffixDefinition definition = EquipmentAffixDefinition.fromJson(entry.getValue().getAsJsonObject());
                    if (definition != null) {
                        definitions.add(definition);
                    }
                } catch (RuntimeException exception) {
                    EquipmentQualityMod.LOGGER.warn("Failed to parse affix definition {}", entry.getKey(), exception);
                }
            }

            EquipmentAffixDefinition.replaceLoadedDefinitions(definitions);
            EquipmentQualityMod.LOGGER.info("Loaded {} affix definitions", definitions.size());
        }
    }

    private static final class SkillReloadListener extends SimpleJsonResourceReloadListener {
        private SkillReloadListener() {
            super(GSON, "skills");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<EquipmentSkillDefinition> definitions = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    EquipmentQualityMod.LOGGER.warn("Skipping skill definition {} because it is not a JSON object", entry.getKey());
                    continue;
                }

                try {
                    EquipmentSkillDefinition definition = EquipmentSkillDefinition.fromJson(entry.getValue().getAsJsonObject());
                    if (definition != null) {
                        definitions.add(definition);
                    }
                } catch (RuntimeException exception) {
                    EquipmentQualityMod.LOGGER.warn("Failed to parse skill definition {}", entry.getKey(), exception);
                }
            }

            EquipmentSkillDefinition.replaceLoadedDefinitions(definitions);
            EquipmentQualityMod.LOGGER.info("Loaded {} skill definitions", definitions.size());
        }
    }
}