package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class NamespaceHeaderWidget extends Button {
    private static final ResourceLocation CHEVRON_COLLAPSED = Polytone.res("config/section_collapsed");
    private static final ResourceLocation CHEVRON_EXPANDED = Polytone.res("config/section_expanded");
    // 7x7 native: one shy of the 8px glyph, since bold text gains a pixel and would otherwise overpower it.
    private static final int CHEVRON_SIZE = 7;
    private static final int TEXT_GAP = 3;

    private final Component boldTitle;
    private final boolean expanded;

    NamespaceHeaderWidget(int width, int height, Component title, boolean expanded, OnPress onPress) {
        super(0, 0, width, height, title, onPress, DEFAULT_NARRATION);
        this.boldTitle = title.copy().withStyle(ChatFormatting.BOLD);
        this.expanded = expanded;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = this.active && this.isHoveredOrFocused();
        if (highlighted) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(),
                    this.getY() + this.getHeight(), 0x22FFFFFF);
        }

        int color = !this.active ? 0xA0A0A0 : highlighted ? 0xFFFFA0 : 0xFFFFFF;

        // Vanilla vertical text centering (AbstractWidget#renderScrollingString): (h - 9)/2 + 1.
        // Both the 8px glyph and the 7px chevron share this top so they line up on the pixel grid.
        int contentTop = this.getY() + (this.getHeight() - 9) / 2 + 1;

        int chevronX = this.getX() + 1;
        guiGraphics.blitSprite(this.expanded ? CHEVRON_EXPANDED : CHEVRON_COLLAPSED,
                chevronX, contentTop, CHEVRON_SIZE, CHEVRON_SIZE);

        Font font = Minecraft.getInstance().font;
        int textX = chevronX + CHEVRON_SIZE + TEXT_GAP;
        guiGraphics.drawString(font, this.boldTitle, textX, contentTop, 0xFF000000 | color);
    }
}
