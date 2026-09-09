package net.mehvahdjukaar.polytone.compat.nautilus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

// drawing and hit testing shared by the editor pick overlays
final class OverlayHelper {

    static final int SLOT = 16;

    private static final int LABEL_BG = 0xE0_000000;
    private static final int LABEL_TEXT = 0xFF_FFFFFF;
    private static final int TARGETED = 0xFF_3BE06B;
    private static final int UNTARGETED = 0xFF_FFAA33;
    private static final int MUTED = 0xFF_B0B0B0;

    @Nullable
    static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getMenu().slots) {
            if (insideSlot((int) mouseX, (int) mouseY, screen.leftPos + slot.x, screen.topPos + slot.y)) return slot;
        }
        return null;
    }

    static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static boolean insideSlot(int mx, int my, int x, int y) {
        return inside(mx, my, x, y, SLOT, SLOT);
    }

    static void box(GuiGraphics graphics, int x, int y, int w, int h, int fillColor, int outlineColor) {
        if (fillColor != 0) graphics.fill(x, y, x + w, y + h, fillColor);
        graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, outlineColor);
    }

    static void slotBox(GuiGraphics graphics, int x, int y, int fillColor, int outlineColor) {
        box(graphics, x, y, SLOT, SLOT, fillColor, outlineColor);
    }

    // two line header in the top left: what the open editor file targets, then what to do about it
    static void banner(GuiGraphics graphics, boolean targeted, String subject, String detail) {
        String head = (targeted ? "[x] targeted" : "[ ] not targeted") + "  -  " + subject;
        Font font = Minecraft.getInstance().font;
        int w = Math.max(font.width(head), font.width(detail));
        int x = 4, y = 4;
        graphics.fill(x, y, x + w + 8, y + font.lineHeight * 2 + 6, LABEL_BG);
        graphics.drawString(font, head, x + 4, y + 3, targeted ? TARGETED : UNTARGETED, false);
        graphics.drawString(font, detail, x + 4, y + 3 + font.lineHeight + 1, MUTED, false);
    }

    static void label(GuiGraphics graphics, String text, int anchorX, int anchorY) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text);
        int ly = anchorY - font.lineHeight - 3;
        if (ly < 2) ly = anchorY + SLOT + 3; //flip below when there's no room above
        graphics.fill(anchorX - 2, ly - 2, anchorX + w + 2, ly + font.lineHeight, LABEL_BG);
        graphics.drawString(font, text, anchorX, ly, LABEL_TEXT, false);
    }
}
