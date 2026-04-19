package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + EquipmentQualityMod.MOD_ID;
    public static final KeyMapping OPEN_DETAILS = new KeyMapping(
        "key." + EquipmentQualityMod.MOD_ID + ".open_details",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        CATEGORY
    );

    private ModKeyMappings() {
    }
}