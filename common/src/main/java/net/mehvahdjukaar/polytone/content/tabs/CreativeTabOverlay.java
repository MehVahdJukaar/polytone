package net.mehvahdjukaar.polytone.content.tabs;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusCreativeTabOverlay;
import net.mehvahdjukaar.polytone.mixins.accessor.AbstractContainerScreenAccessor;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Editor-only inspector over the creative screen while picking: removals washed red, additions green,
// already picked amber. Clicking reports the item to the editor instead of grabbing it.
public final class CreativeTabOverlay {

    private static final int SLOT = 16;

    private static final int REMOVED_FILL = 0x55_FF4D4D;
    private static final int REMOVED_OUTLINE = 0xDD_FF4D4D;
    private static final int ADDED_FILL = 0x44_3BE06B;
    private static final int ADDED_OUTLINE = 0xDD_3BE06B;
    private static final int PENDING_OUTLINE = 0xFF_FFCC33;
    private static final int HOVER_OUTLINE = 0xFF_FFFFFF;

    private static final int LABEL_BG = 0xE0_000000;
    private static final int TARGETED = 0xFF_3BE06B;
    private static final int UNTARGETED = 0xFF_FFAA33;
    private static final int MUTED = 0xFF_B0B0B0;

    public static void render(GuiGraphics graphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        CreativeTabModifier mod = NautilusCreativeTabOverlay.edited();
        Identifier tabId = NautilusCreativeTabOverlay.openTab();
        boolean targeted = NautilusCreativeTabOverlay.targets(tabId);

        List<ItemPredicate> removals = mod == null ? List.of() : mod.removals();
        List<AddedSet> additions = mod == null ? List.of() : addedSets(mod);

        int leftPos = ((AbstractContainerScreenAccessor) screen).polytone$getLeftPos();
        int topPos = ((AbstractContainerScreenAccessor) screen).polytone$getTopPos();

        // Vanilla already captions the hovered item, so the overlay only outlines it.
        boolean hovering = false;
        int hoverX = 0, hoverY = 0;

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !isTabSlot(slot)) continue;
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;

            if (targeted && NautilusCreativeTabOverlay.matchesRemoval(removals, stack)) {
                box(graphics, sx, sy, REMOVED_FILL, REMOVED_OUTLINE);
            } else if (targeted && isAdded(additions, stack)) {
                box(graphics, sx, sy, ADDED_FILL, ADDED_OUTLINE);
            } else if (NautilusCreativeTabOverlay.isPending(stack.getItem())) {
                box(graphics, sx, sy, 0, PENDING_OUTLINE);
            }

            if (inside(mouseX, mouseY, sx, sy)) {
                hovering = true;
                hoverX = sx;
                hoverY = sy;
            }
        }

        drawBanner(graphics, tabId, targeted);

        if (hovering) box(graphics, hoverX, hoverY, 0, HOVER_OUTLINE);
    }

    @Nullable
    public static ItemStack pickAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = ((AbstractContainerScreenAccessor) screen).polytone$getLeftPos();
        int topPos = ((AbstractContainerScreenAccessor) screen).polytone$getTopPos();
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && isTabSlot(slot)
                    && inside((int) mouseX, (int) mouseY, leftPos + slot.x, topPos + slot.y)) {
                return stack;
            }
        }
        return null;
    }

    // The hotbar strip under the grid is the player's inventory, not tab contents - a modifier can't
    // touch those items, so they are neither marked nor pickable.
    private static boolean isTabSlot(Slot slot) {
        return !(slot.container instanceof Inventory);
    }

    // An addition's item list resolved once per frame - a tag-backed one would otherwise be re-resolved
    // for every slot. `inverse` flips the meaning to "everything but these", like the reload path.
    private record AddedSet(Set<Item> items, boolean inverse) {
    }

    private static List<AddedSet> addedSets(CreativeTabModifier mod) {
        List<AddedSet> out = new ArrayList<>();
        for (ItemAddition addition : mod.additions()) {
            List<ItemStack> stacks = addition.items().get();
            if (stacks == null) continue;
            Set<Item> items = new HashSet<>();
            for (ItemStack s : stacks) items.add(s.getItem());
            out.add(new AddedSet(items, addition.inverse()));
        }
        return out;
    }

    private static boolean isAdded(List<AddedSet> sets, ItemStack stack) {
        for (AddedSet set : sets) {
            if (set.items.contains(stack.getItem()) != set.inverse) return true;
        }
        return false;
    }

    private static void drawBanner(GuiGraphics graphics, @Nullable Identifier tabId, boolean targeted) {
        String head = (targeted ? "● Targeted" : "○ Not targeted") + "   ·   " + (tabId == null ? "?" : tabId);
        int selected = NautilusCreativeTabOverlay.pendingCount();
        String detail;
        if (!targeted) {
            detail = "this modifier doesn't target the open tab";
        } else if (selected == 0) {
            detail = "click items to select them";
        } else {
            detail = StrUtils.plural(selected, "item") + " selected   ·   click again to unselect";
        }

        Font font = Minecraft.getInstance().font;
        int w = Math.max(font.width(head), font.width(detail));
        int x = 4, y = 4;
        graphics.fill(x, y, x + w + 8, y + font.lineHeight * 2 + 6, LABEL_BG);
        graphics.drawString(font, head, x + 4, y + 3, targeted ? TARGETED : UNTARGETED, false);
        graphics.drawString(font, detail, x + 4, y + 3 + font.lineHeight + 1, MUTED, false);
    }

    private static void box(GuiGraphics graphics, int x, int y, int fillColor, int outlineColor) {
        if (fillColor != 0) graphics.fill(x, y, x + SLOT, y + SLOT, fillColor);
        graphics.renderOutline(x - 1, y - 1, SLOT + 2, SLOT + 2, outlineColor);
    }

    private static boolean inside(int mx, int my, int x, int y) {
        return mx >= x && mx < x + SLOT && my >= y && my < y + SLOT;
    }
}
