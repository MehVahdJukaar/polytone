package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview.PickedElement;
import net.mehvahdjukaar.polytone.content.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.content.slotify.WidgetModifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NautilusGuiModifierOverlay {

    private static final int SLOT_OUTLINE = 0x55_3AA0FF;
    private static final int WIDGET_OUTLINE = 0x55_C07BFF;
    private static final int MOD_FILL = 0x33_3BE06B;
    private static final int MOD_OUTLINE = 0xDD_3BE06B;
    private static final int HOVER_FILL = 0x44_FFCC33;
    private static final int HOVER_OUTLINE = 0xFF_FFCC33;

    public static void render(GuiGraphicsExtractor graphics, Screen screen, int mouseX, int mouseY) {
        ScreenModifier mod = Polytone.SLOTIFY.getGuiModifier(screen);
        List<WidgetModifier> widgetMods = mod != null ? mod.widgetModifiers() : List.of();

        AbstractWidget hoveredWidget = null;
        int modifiedWidgets = 0;
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget w) || !w.visible) continue;
            boolean modified = matchesAny(widgetMods, w);
            if (modified) modifiedWidgets++;
            OverlayHelper.box(graphics, w.getX(), w.getY(), w.getWidth(), w.getHeight(),
                    modified ? MOD_FILL : 0, modified ? MOD_OUTLINE : WIDGET_OUTLINE);
            if (OverlayHelper.inside(mouseX, mouseY, w.getX(), w.getY(), w.getWidth(), w.getHeight())) hoveredWidget = w;
        }

        Slot hoveredSlot = null;
        int leftPos = 0, topPos = 0, modifiedSlots = 0;
        if (screen instanceof AbstractContainerScreen<?> cs) {
            leftPos = OverlayHelper.leftPos(cs);
            topPos = OverlayHelper.topPos(cs);
            for (Slot slot : cs.getMenu().slots) {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                boolean modified = !Polytone.SLOTIFY.getSlotModifiers(cs, slot).isEmpty();
                if (modified) modifiedSlots++;
                OverlayHelper.slotBox(graphics, sx, sy, modified ? MOD_FILL : 0, modified ? MOD_OUTLINE : SLOT_OUTLINE);
                if (OverlayHelper.insideSlot(mouseX, mouseY, sx, sy)) hoveredSlot = slot;
            }
        }

        drawBanner(graphics, screen, mod != null, modifiedSlots, modifiedWidgets);

        // Hover caption - widget wins when both overlap (widgets sit on top of the panel).
        if (hoveredWidget != null) {
            OverlayHelper.box(graphics, hoveredWidget.getX(), hoveredWidget.getY(),
                    hoveredWidget.getWidth(), hoveredWidget.getHeight(), HOVER_FILL, HOVER_OUTLINE);
            OverlayHelper.label(graphics, widgetLabel(screen, hoveredWidget), hoveredWidget.getX(), hoveredWidget.getY());
        } else if (hoveredSlot != null) {
            int sx = leftPos + hoveredSlot.x;
            int sy = topPos + hoveredSlot.y;
            OverlayHelper.slotBox(graphics, sx, sy, HOVER_FILL, HOVER_OUTLINE);
            OverlayHelper.label(graphics, slotLabel(screen, hoveredSlot, sx, sy), sx, sy);
        }
    }

    @Nullable
    public static PickedElement pickAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot slot = OverlayHelper.slotAt(screen, mouseX, mouseY);
        if (slot == null) return null;
        int sx = OverlayHelper.leftPos(screen) + slot.x;
        int sy = OverlayHelper.topPos(screen) + slot.y;
        return new PickedElement(slot.index, sx - screen.width / 2, sy - screen.height / 2,
                OverlayHelper.SLOT, OverlayHelper.SLOT, slot.getClass().getName());
    }

    private static void drawBanner(GuiGraphicsExtractor graphics, Screen screen, boolean targeted, int modSlots, int modWidgets) {
        GuiModifierPreview.DetectedTarget t = GuiModifierPreview.targetOf(screen);
        String subject = t == null ? "?" : t.type().getSerializedName() + " = " + t.target();

        String detail;
        if (!targeted) {
            detail = "no modifier matches this screen yet";
        } else if (modSlots == 0 && modWidgets == 0) {
            detail = "no elements matched";
        } else {
            detail = "modifying " + StrUtils.plural(modSlots, "slot")
                    + (modWidgets > 0 ? ", " + StrUtils.plural(modWidgets, "widget") : "");
        }
        if (targeted && GuiModifierPreview.isPreviewing(screen)) detail += "   (live preview)";

        OverlayHelper.banner(graphics, targeted, subject, detail);
    }

    private static String slotLabel(Screen screen, Slot slot, int sx, int sy) {
        return "slot #" + slot.index + "  (" + (sx - screen.width / 2) + ", " + (sy - screen.height / 2) + ")  "
                + StrUtils.simpleName(slot.getClass().getName());
    }

    private static String widgetLabel(Screen screen, AbstractWidget w) {
        int cx = w.getX() - screen.width / 2;
        int cy = w.getY() - screen.height / 2;
        String msg = w.getMessage().getString();
        String named = msg.isBlank() ? "" : "\"" + msg + "\"  ";
        return "widget  " + named + StrUtils.simpleName(w.getClass().getName()) + "  (" + cx + ", " + cy + ")  "
                + w.getWidth() + "x" + w.getHeight();
    }

    private static boolean matchesAny(List<WidgetModifier> mods, AbstractWidget w) {
        for (WidgetModifier m : mods) {
            if (m.matches(w)) return true;
        }
        return false;
    }
}
