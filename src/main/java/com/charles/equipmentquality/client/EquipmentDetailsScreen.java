package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityData;
import com.charles.equipmentquality.EquipmentQualityMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class EquipmentDetailsScreen extends Screen {
    private final ItemStack stack;

    public EquipmentDetailsScreen(ItemStack stack) {
        super(Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details"));
        this.stack = stack.copy();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Intentionally empty to avoid Minecraft's default screen blur.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = 286;
        int panelHeight = 192;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xD0101010);
        guiGraphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 20, 0xC0202020);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);
        guiGraphics.renderItem(this.stack, left + 10, top + 24);
        guiGraphics.drawString(this.font, this.stack.getHoverName(), left + 32, top + 28, 0xFFFFFF, false);

        List<EquipmentQualityData.DetailSection> sections = EquipmentQualityData.getDetailSections(this.stack);
        int y = top + 50;
        for (EquipmentQualityData.DetailSection section : sections) {
            guiGraphics.drawString(this.font, section.title(), left + 12, y, 0xF4D35E, false);
            y += 12;

            for (Component detailLine : section.lines()) {
                guiGraphics.drawString(this.font, detailLine, left + 18, y, 0xE0E0E0, false);
                y += 11;
            }

            y += 5;
        }

        guiGraphics.drawCenteredString(this.font, Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.close_hint"), this.width / 2, top + panelHeight - 12, 0xA0A0A0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}