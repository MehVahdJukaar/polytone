package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

class NamespaceHeaderWidget extends Button {
    private static final int CHEVRON_SIZE = 7;
    private static final int TEXT_GAP = 5;

    private final Component boldTitle;
    private final boolean expanded;

    NamespaceHeaderWidget(int width, int height, Component title, boolean expanded, OnPress onPress) {
        super(0, 0, width, height, title, onPress, DEFAULT_NARRATION);
        this.boldTitle = title.copy().withStyle(ChatFormatting.BOLD);
        this.expanded = expanded;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = this.active && this.isHoveredOrFocused();
        // Faint full-row highlight so the header reads as a clickable strip, vanilla list-row style.
        if (highlighted) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(),
                    this.getY() + this.getHeight(), 0x22FFFFFF);
        }

        int color = !this.active ? 0xA0A0A0 : highlighted ? 0xFFFFA0 : 0xFFFFFF;

        int chevronX = this.getX() + 1;
        int chevronY = this.getY() + (this.getHeight() - CHEVRON_SIZE) / 2;
        drawChevron(guiGraphics, chevronX, chevronY, this.expanded, 0xFF000000 | color);

        Font font = Minecraft.getInstance().font;
        int textX = chevronX + CHEVRON_SIZE + TEXT_GAP;
        int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2;
        guiGraphics.drawString(font, this.boldTitle, textX, textY, 0xFF000000 | color);
    }

    private static void drawChevron(GuiGraphics guiGraphics, int x, int y, boolean expanded, int argb) {
        int half = CHEVRON_SIZE / 2; // 3
        for (int i = 0; i <= half; i++) {
            if (expanded) {
                guiGraphics.fill(x + i, y + i, x + CHEVRON_SIZE - i, y + i + 1, argb);
            } else {
                guiGraphics.fill(x + i, y + i, x + i + 1, y + CHEVRON_SIZE - i, argb);
            }
        }
    }
}
