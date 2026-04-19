package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityData;
import com.charles.equipmentquality.EquipmentQualityMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
        int panelWidth = 364;
        int panelHeight = 228;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int headerHeight = 30;
        int footerHeight = 18;
        int contentTop = top + headerHeight + 26;
        int contentBottom = top + panelHeight - footerHeight - 8;
        int columnGap = 14;
        int columnWidth = (panelWidth - 24 - columnGap) / 2;
        int leftColumnX = left + 12;
        int rightColumnX = leftColumnX + columnWidth + columnGap;

        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0101216);
        guiGraphics.fill(left + 1, top + 1, left + panelWidth - 1, top + panelHeight - 1, 0xF01A1E24);
        guiGraphics.fill(left + 1, top + 1, left + panelWidth - 1, top + headerHeight, 0xF0242A33);
        guiGraphics.fill(left, top, left + panelWidth, top + 1, 0xFF607D9A);
        guiGraphics.fill(left, top, left + 1, top + panelHeight, 0xFF607D9A);
        guiGraphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, 0xFF0C0F13);
        guiGraphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, 0xFF0C0F13);
        guiGraphics.fill(left + panelWidth / 2, contentTop - 8, left + panelWidth / 2 + 1, contentBottom, 0x503D4754);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);
        guiGraphics.renderItem(this.stack, left + 12, top + 38);
        guiGraphics.drawString(this.font, trimToWidth(this.stack.getHoverName(), panelWidth - 56), left + 34, top + 42, 0xFFFFFF, false);

        List<EquipmentQualityData.DetailSection> sections = EquipmentQualityData.getDetailSections(this.stack);
        int leftY = contentTop;
        int rightY = contentTop;

        if (sections.size() > 0) {
            leftY = drawSection(guiGraphics, sections.get(0), leftColumnX, leftY, columnWidth, contentBottom);
        }
        if (sections.size() > 1) {
            leftY = drawSection(guiGraphics, sections.get(1), leftColumnX, leftY, columnWidth, contentBottom);
        }
        if (sections.size() > 2) {
            rightY = drawSection(guiGraphics, sections.get(2), rightColumnX, rightY, columnWidth, contentBottom);
        }
        if (sections.size() > 3) {
            rightY = drawSection(guiGraphics, sections.get(3), rightColumnX, rightY, columnWidth, contentBottom);
        }
        if (sections.size() > 4) {
            int footerTop = Math.max(leftY, rightY) + 2;
            drawSection(guiGraphics, sections.get(4), leftColumnX, footerTop, panelWidth - 24, contentBottom);
        }

        guiGraphics.drawCenteredString(this.font, Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.close_hint"), this.width / 2, top + panelHeight - 12, 0xA0A0A0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private int drawSection(GuiGraphics guiGraphics, EquipmentQualityData.DetailSection section, int x, int y, int width, int maxBottom) {
        if (y >= maxBottom) {
            return y;
        }

        guiGraphics.drawString(this.font, trimToWidth(section.title(), width), x, y, 0xF4D35E, false);
        y += 12;

        for (Component detailLine : section.lines()) {
            List<FormattedCharSequence> wrappedLines = this.font.split(detailLine, width - 6);
            for (FormattedCharSequence wrappedLine : wrappedLines) {
                if (y + 9 > maxBottom) {
                    guiGraphics.drawString(this.font, Component.literal("..."), x + 6, maxBottom - 9, 0x909090, false);
                    return maxBottom;
                }

                guiGraphics.drawString(this.font, wrappedLine, x + 6, y, 0xE0E0E0, false);
                y += 10;
            }
            y += 3;
        }

        return y + 4;
    }

    private Component trimToWidth(Component text, int width) {
        String trimmed = this.font.plainSubstrByWidth(text.getString(), Math.max(0, width - this.font.width("...")));
        if (trimmed.length() < text.getString().length()) {
            trimmed += "...";
        }
        return Component.literal(trimmed).withStyle(text.getStyle());
    }
}