package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.polytone.mixins.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

final class OverlayHelper {

    static final int SLOT = 16;

    private static final int LABEL_BG = 0xE0_000000;
    private static final int LABEL_TEXT = 0xFF_FFFFFF;
    private static final int TARGETED = 0xFF_3BE06B;
    private static final int UNTARGETED = 0xFF_FFAA33;
    private static final int MUTED = 0xFF_B0B0B0;

    static int leftPos(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).polytone$getLeftPos();
    }

    static int topPos(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).polytone$getTopPos();
    }

    @Nullable
    static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = leftPos(screen);
        int topPos = topPos(screen);
        for (Slot slot : screen.getMenu().slots) {
            if (insideSlot((int) mouseX, (int) mouseY, leftPos + slot.x, topPos + slot.y)) return slot;
        }
        return null;
    }

    static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static boolean insideSlot(int mx, int my, int x, int y) {
        return inside(mx, my, x, y, SLOT, SLOT);
    }

    static void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fillColor, int outlineColor) {
        if (fillColor != 0) graphics.fill(x, y, x + w, y + h, fillColor);
        graphics.outline(x - 1, y - 1, w + 2, h + 2, outlineColor);
    }

    static void slotBox(GuiGraphicsExtractor graphics, int x, int y, int fillColor, int outlineColor) {
        box(graphics, x, y, SLOT, SLOT, fillColor, outlineColor);
    }

    static void banner(GuiGraphicsExtractor graphics, boolean targeted, String subject, String detail) {
        String head = (targeted ? "● Targeted" : "○ Not targeted") + "   ·   " + subject;
        Font font = Minecraft.getInstance().font;
        int w = Math.max(font.width(head), font.width(detail));
        int x = 4, y = 4;
        graphics.fill(x, y, x + w + 8, y + font.lineHeight * 2 + 6, LABEL_BG);
        graphics.text(font, head, x + 4, y + 3, targeted ? TARGETED : UNTARGETED, false);
        graphics.text(font, detail, x + 4, y + 3 + font.lineHeight + 1, MUTED, false);
    }

    static void label(GuiGraphicsExtractor graphics, String text, int anchorX, int anchorY) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text);
        int ly = anchorY - font.lineHeight - 3;
        if (ly < 2) ly = anchorY + SLOT + 3;
        graphics.fill(anchorX - 2, ly - 2, anchorX + w + 2, ly + font.lineHeight, LABEL_BG);
        graphics.text(font, text, anchorX, ly, LABEL_TEXT, false);
    }
}
