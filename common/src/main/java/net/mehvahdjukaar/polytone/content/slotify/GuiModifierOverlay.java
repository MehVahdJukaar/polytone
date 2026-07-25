package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview.PickedElement;
import net.mehvahdjukaar.polytone.mixins.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Editor-only inspector overlay drawn on top of a live screen while picking is enabled. It answers the
 * two questions that are hardest to author blind: <b>is this screen targeted</b> by a modifier (banner,
 * top-left), and <b>which elements does it actually touch</b> - matched slots and widgets are tinted
 * green, everything else gets a faint outline. Hovering any slot/widget captions it with the data
 * needed to target it (index/message/position/class). It renders on any screen (widgets everywhere,
 * slots on container screens) and never mutates anything - it only reads and draws.
 */
public final class GuiModifierOverlay {

    private static final int SLOT = 16;

    // Resting outlines: slots blue, widgets violet - so the two element kinds read apart at a glance.
    private static final int SLOT_OUTLINE = 0x55_3AA0FF;
    private static final int WIDGET_OUTLINE = 0x55_C07BFF;
    // "This element is modified": green wash + solid green box, shared by slots and widgets.
    private static final int MOD_FILL = 0x33_3BE06B;
    private static final int MOD_OUTLINE = 0xDD_3BE06B;
    // Hovered element: amber wash + box, drawn over everything else.
    private static final int HOVER_FILL = 0x44_FFCC33;
    private static final int HOVER_OUTLINE = 0xFF_FFCC33;

    private static final int LABEL_BG = 0xE0_000000;
    private static final int LABEL_TEXT = 0xFF_FFFFFF;
    private static final int TARGETED = 0xFF_3BE06B;   // green
    private static final int UNTARGETED = 0xFF_FFAA33; // amber
    private static final int MUTED = 0xFF_B0B0B0;

    public static void render(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        ScreenModifier mod = Polytone.SLOTIFY.getGuiModifier(screen);
        List<WidgetModifier> widgetMods = mod != null ? mod.widgetModifiers() : List.of();

        // Widgets (present on every screen kind).
        AbstractWidget hoveredWidget = null;
        int modifiedWidgets = 0;
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget w) || !w.visible) continue;
            boolean modified = matchesAny(widgetMods, w);
            if (modified) modifiedWidgets++;
            box(graphics, w.getX(), w.getY(), w.getWidth(), w.getHeight(), modified ? MOD_FILL : 0, modified ? MOD_OUTLINE : WIDGET_OUTLINE);
            if (inside(mouseX, mouseY, w.getX(), w.getY(), w.getWidth(), w.getHeight())) hoveredWidget = w;
        }

        // Slots (container screens only).
        Slot hoveredSlot = null;
        int leftPos = 0, topPos = 0, modifiedSlots = 0;
        if (screen instanceof AbstractContainerScreen<?> cs) {
            leftPos = ((AbstractContainerScreenAccessor) cs).polytone$getLeftPos();
            topPos = ((AbstractContainerScreenAccessor) cs).polytone$getTopPos();
            for (Slot slot : cs.getMenu().slots) {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                boolean modified = !Polytone.SLOTIFY.getSlotModifiers(cs, slot).isEmpty();
                if (modified) modifiedSlots++;
                box(graphics, sx, sy, SLOT, SLOT, modified ? MOD_FILL : 0, modified ? MOD_OUTLINE : SLOT_OUTLINE);
                if (inside(mouseX, mouseY, sx, sy, SLOT, SLOT)) hoveredSlot = slot;
            }
        }

        drawBanner(graphics, screen, mod != null, modifiedSlots, modifiedWidgets);

        // Hover caption - widget wins when both overlap (widgets sit on top of the panel).
        if (hoveredWidget != null) {
            box(graphics, hoveredWidget.getX(), hoveredWidget.getY(), hoveredWidget.getWidth(), hoveredWidget.getHeight(),
                    HOVER_FILL, HOVER_OUTLINE);
            drawLabel(graphics, widgetLabel(screen, hoveredWidget), hoveredWidget.getX(), hoveredWidget.getY());
        } else if (hoveredSlot != null) {
            int sx = leftPos + hoveredSlot.x;
            int sy = topPos + hoveredSlot.y;
            box(graphics, sx, sy, SLOT, SLOT, HOVER_FILL, HOVER_OUTLINE);
            drawLabel(graphics, slotLabel(screen, hoveredSlot, leftPos, topPos), sx, sy);
        }
    }

    // --- banner ---------------------------------------------------------------------------------

    private static void drawBanner(GuiGraphics graphics, Screen screen, boolean targeted, int modSlots, int modWidgets) {
        GuiModifierPreview.DetectedTarget t = GuiModifierPreview.targetOf(screen);
        String target = t == null ? "?" : t.type().getSerializedName() + " = " + t.target();

        String head = (targeted ? "● Targeted" : "○ Not targeted") + "   ·   " + target;
        String detail;
        if (targeted) {
            String touch = modSlots == 0 && modWidgets == 0
                    ? "no elements matched"
                    : "modifying " + StrUtils.plural(modSlots, "slot") + (modWidgets > 0 ? ", " + StrUtils.plural(modWidgets, "widget") : "");
            detail = touch + (GuiModifierPreview.isPreviewing(screen) ? "   (live preview)" : "");
        } else {
            detail = "no modifier matches this screen yet";
        }

        Font font = Minecraft.getInstance().font;
        int w = Math.max(font.width(head), font.width(detail));
        int x = 4, y = 4;
        int h = font.lineHeight * 2 + 6;
        graphics.fill(x, y, x + w + 8, y + h, LABEL_BG);
        graphics.drawString(font, head, x + 4, y + 3, targeted ? TARGETED : UNTARGETED, false);
        graphics.drawString(font, detail, x + 4, y + 3 + font.lineHeight + 1, MUTED, false);
    }

    // --- element labels -------------------------------------------------------------------------

    private static void drawLabel(GuiGraphics graphics, String text, int anchorX, int anchorY) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text);
        int ly = anchorY - font.lineHeight - 3;
        if (ly < 2) ly = anchorY + SLOT + 3; // flip below when there's no room above
        graphics.fill(anchorX - 2, ly - 2, anchorX + w + 2, ly + font.lineHeight, LABEL_BG);
        graphics.drawString(font, text, anchorX, ly, LABEL_TEXT, false);
    }

    private static String slotLabel(Screen screen, Slot slot, int leftPos, int topPos) {
        int cx = leftPos + slot.x - screen.width / 2;
        int cy = topPos + slot.y - screen.height / 2;
        return "slot #" + slot.index + "  (" + cx + ", " + cy + ")  " + StrUtils.simpleName(slot.getClass().getName());
    }

    private static String widgetLabel(Screen screen, AbstractWidget w) {
        int cx = w.getX() - screen.width / 2;
        int cy = w.getY() - screen.height / 2;
        String msg = w.getMessage().getString();
        String named = msg.isBlank() ? "" : "\"" + msg + "\"  ";
        return "widget  " + named + StrUtils.simpleName(w.getClass().getName()) + "  (" + cx + ", " + cy + ")  "
                + w.getWidth() + "x" + w.getHeight();
    }

    // --- shared hit-test used by the click handler ----------------------------------------------

    /** The slot under the cursor as a {@link PickedElement}, or null. Shared with the click mixin. */
    @Nullable
    public static PickedElement pickAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = ((AbstractContainerScreenAccessor) screen).polytone$getLeftPos();
        int topPos = ((AbstractContainerScreenAccessor) screen).polytone$getTopPos();
        for (Slot slot : screen.getMenu().slots) {
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (inside((int) mouseX, (int) mouseY, sx, sy, SLOT, SLOT)) {
                int cx = sx - screen.width / 2;
                int cy = sy - screen.height / 2;
                return new PickedElement(slot.index, cx, cy, SLOT, SLOT, slot.getClass().getName());
            }
        }
        return null;
    }

    // --- the shared screen-render pass (overlay + centered sprites), called by both platforms -----

    /**
     * Full editor render pass for one screen, shared by both platform screen-render hooks (only the
     * event wiring differs): the inspector overlay in absolute screen space when picking is on, then
     * the modifier's extra sprites/texts anchored to the screen center. {@code renderExtraSprites}
     * no-ops when the screen has no modifier, so this is safe to call unconditionally every frame.
     */
    public static void renderScreenExtras(GuiGraphics graphics, SlotifyScreen ss,
                                          int screenWidth, int screenHeight,
                                          int mouseX, int mouseY, float partialTick) {
        if (GuiModifierPreview.isPickingEnabled() && ss instanceof Screen screen) {
            render(graphics, screen, mouseX, mouseY);
        }
        // 1.21.11 GUI transform stack is the 2D Matrix3x2fStack (no Z), unlike 1.21.1's PoseStack.
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.identity();
        pose.translate(screenWidth / 2F, screenHeight / 2F);
        ss.polytone$renderExtraSprites(graphics, mouseX, mouseY, partialTick);
        pose.popMatrix();
    }

    // --- helpers --------------------------------------------------------------------------------

    private static boolean matchesAny(List<WidgetModifier> mods, AbstractWidget w) {
        for (WidgetModifier m : mods) {
            if (m.matches(w)) return true;
        }
        return false;
    }

    // Fill (when fillColor != 0) + a 1px outline just outside the element bounds.
    private static void box(GuiGraphics graphics, int x, int y, int w, int h, int fillColor, int outlineColor) {
        if (fillColor != 0) graphics.fill(x, y, x + w, y + h, fillColor);
        graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, outlineColor);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
