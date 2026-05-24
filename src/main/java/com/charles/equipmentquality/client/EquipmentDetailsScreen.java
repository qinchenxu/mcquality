package com.charles.equipmentquality.client;

import com.charles.equipmentquality.EquipmentQualityData;
import com.charles.equipmentquality.EquipmentQualityMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class EquipmentDetailsScreen extends Screen {
    private static final int PANEL_WIDTH = 364;
    private static final int PANEL_HEIGHT = 228;
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 18;
    private static final int COLUMN_GAP = 14;
    private static final int SCROLL_STEP = 16;

    private final ItemStack stack;
    private int scrollOffset;
    private int maxScroll;

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
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int contentTop = top + HEADER_HEIGHT + 26;
        int contentBottom = top + PANEL_HEIGHT - FOOTER_HEIGHT - 8;
        int columnWidth = (PANEL_WIDTH - 24 - COLUMN_GAP) / 2;
        int leftColumnX = left + 12;
        int rightColumnX = leftColumnX + columnWidth + COLUMN_GAP;
        int visibleHeight = contentBottom - contentTop;

        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0101216);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF01A1E24);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + HEADER_HEIGHT, 0xF0242A33);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0xFF607D9A);
        guiGraphics.fill(left, top, left + 1, top + PANEL_HEIGHT, 0xFF607D9A);
        guiGraphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF0C0F13);
        guiGraphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF0C0F13);
        guiGraphics.fill(left + PANEL_WIDTH / 2, contentTop - 8, left + PANEL_WIDTH / 2 + 1, contentBottom, 0x503D4754);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);
        guiGraphics.renderItem(this.stack, left + 12, top + 38);
        guiGraphics.drawString(this.font, trimToWidth(this.stack.getHoverName(), PANEL_WIDTH - 56), left + 34, top + 42, 0xFFFFFF, false);

        List<EquipmentQualityData.DetailSection> sections = EquipmentQualityData.getDetailSections(this.stack);
        int contentHeight = measureContentHeight(sections, columnWidth, PANEL_WIDTH - 24);
        this.maxScroll = Math.max(0, contentHeight - visibleHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

        int leftY = contentTop - this.scrollOffset;
        int rightY = contentTop - this.scrollOffset;

        guiGraphics.enableScissor(left + 2, contentTop, left + PANEL_WIDTH - 2, contentBottom);

        if (sections.size() > 0) {
            leftY = drawSection(guiGraphics, sections.get(0), leftColumnX, leftY, columnWidth);
        }
        if (sections.size() > 1) {
            leftY = drawSection(guiGraphics, sections.get(1), leftColumnX, leftY, columnWidth);
        }
        if (sections.size() > 2) {
            leftY = drawSection(guiGraphics, sections.get(2), leftColumnX, leftY, columnWidth);
        }
        if (sections.size() > 3) {
            rightY = drawSection(guiGraphics, sections.get(3), rightColumnX, rightY, columnWidth);
        }
        if (sections.size() > 4) {
            rightY = drawSection(guiGraphics, sections.get(4), rightColumnX, rightY, columnWidth);
        }
        if (sections.size() > 5) {
            int footerTop = Math.max(leftY, rightY) + 2;
            drawSection(guiGraphics, sections.get(5), leftColumnX, footerTop, PANEL_WIDTH - 24);
        }

        guiGraphics.disableScissor();

        if (this.maxScroll > 0) {
            drawScrollBar(guiGraphics, left + PANEL_WIDTH - 7, contentTop, contentBottom, visibleHeight, contentHeight);
        }

        guiGraphics.drawCenteredString(this.font, Component.translatable("screen." + EquipmentQualityMod.MOD_ID + ".details.close_hint"), this.width / 2, top + PANEL_HEIGHT - 12, 0xA0A0A0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseInContent(mouseX, mouseY) && scrollBy((int) Math.round(-scrollY * SCROLL_STEP))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> scrollBy(-SCROLL_STEP);
            case GLFW.GLFW_KEY_DOWN -> scrollBy(SCROLL_STEP);
            case GLFW.GLFW_KEY_PAGE_UP -> scrollBy(-SCROLL_STEP * 5);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollBy(SCROLL_STEP * 5);
            case GLFW.GLFW_KEY_HOME -> setScrollOffset(0);
            case GLFW.GLFW_KEY_END -> setScrollOffset(this.maxScroll);
            default -> false;
        };
        return handled || super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int drawSection(GuiGraphics guiGraphics, EquipmentQualityData.DetailSection section, int x, int y, int width) {
        guiGraphics.drawString(this.font, trimToWidth(section.title(), width), x, y, 0xF4D35E, false);
        y += 12;

        for (Component detailLine : section.lines()) {
            List<FormattedCharSequence> wrappedLines = this.font.split(detailLine, width - 6);
            for (FormattedCharSequence wrappedLine : wrappedLines) {
                guiGraphics.drawString(this.font, wrappedLine, x + 6, y, 0xE0E0E0, false);
                y += 10;
            }
            y += 3;
        }

        return y + 4;
    }

    private int measureContentHeight(List<EquipmentQualityData.DetailSection> sections, int columnWidth, int footerWidth) {
        int leftHeight = 0;
        int rightHeight = 0;

        if (sections.size() > 0) {
            leftHeight += measureSectionHeight(sections.get(0), columnWidth);
        }
        if (sections.size() > 1) {
            leftHeight += measureSectionHeight(sections.get(1), columnWidth);
        }
        if (sections.size() > 2) {
            leftHeight += measureSectionHeight(sections.get(2), columnWidth);
        }
        if (sections.size() > 3) {
            rightHeight += measureSectionHeight(sections.get(3), columnWidth);
        }
        if (sections.size() > 4) {
            rightHeight += measureSectionHeight(sections.get(4), columnWidth);
        }

        int contentHeight = Math.max(leftHeight, rightHeight);
        if (sections.size() > 5) {
            contentHeight = Math.max(contentHeight, Math.max(leftHeight, rightHeight) + 2 + measureSectionHeight(sections.get(5), footerWidth));
        }
        return contentHeight;
    }

    private int measureSectionHeight(EquipmentQualityData.DetailSection section, int width) {
        int sectionHeight = 16;
        for (Component detailLine : section.lines()) {
            sectionHeight += this.font.split(detailLine, Math.max(6, width - 6)).size() * 10;
            sectionHeight += 3;
        }
        return sectionHeight;
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int top, int bottom, int visibleHeight, int contentHeight) {
        int trackWidth = 2;
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(18, (int) Math.round((visibleHeight / (double) contentHeight) * trackHeight));
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = top + (this.maxScroll == 0 ? 0 : (int) Math.round((this.scrollOffset / (double) this.maxScroll) * thumbTravel));

        guiGraphics.fill(x, top, x + trackWidth, bottom, 0x60424A56);
        guiGraphics.fill(x, thumbTop, x + trackWidth, thumbTop + thumbHeight, 0xFFC7D4E1);
    }

    private boolean scrollBy(int delta) {
        return setScrollOffset(this.scrollOffset + delta);
    }

    private boolean setScrollOffset(int newOffset) {
        int clamped = Mth.clamp(newOffset, 0, this.maxScroll);
        if (clamped == this.scrollOffset) {
            return false;
        }
        this.scrollOffset = clamped;
        return true;
    }

    private boolean isMouseInContent(double mouseX, double mouseY) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int contentTop = top + HEADER_HEIGHT + 26;
        int contentBottom = top + PANEL_HEIGHT - FOOTER_HEIGHT - 8;
        return mouseX >= left + 8 && mouseX <= left + PANEL_WIDTH - 8 && mouseY >= contentTop && mouseY <= contentBottom;
    }

    private Component trimToWidth(Component text, int width) {
        String fullText = text.getString();
        if (this.font.width(fullText) <= width) {
            return text;
        }

        String trimmed = this.font.plainSubstrByWidth(fullText, Math.max(0, width - this.font.width("...")));
        if (trimmed.length() < fullText.length()) {
            trimmed += "...";
        }
        return Component.literal(trimmed).withStyle(text.getStyle());
    }
}