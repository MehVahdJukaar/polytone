package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabPreview;
import net.mehvahdjukaar.polytone.content.tabs.ItemAddition;
import net.mehvahdjukaar.polytone.content.tabs.ItemPredicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

public final class NautilusCreativeTabOverlay {

    private static final int REMOVED_FILL = 0x55_FF4D4D;
    private static final int REMOVED_OUTLINE = 0xDD_FF4D4D;
    private static final int ADDED_FILL = 0x44_3BE06B;
    private static final int ADDED_OUTLINE = 0xDD_3BE06B;
    private static final int PENDING_OUTLINE = 0xFF_FFCC33;
    private static final int HOVER_OUTLINE = 0xFF_FFFFFF;

    public static void render(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        CreativeTabModifier mod = CreativeTabPreview.edited();
        Identifier tabId = CreativeTabPreview.openTab();
        boolean targeted = CreativeTabPreview.targets(tabId);

        List<ItemPredicate> removals = mod == null ? List.of() : mod.removals();
        List<AddedSet> additions = mod == null ? List.of() : addedSets(mod);

        int leftPos = OverlayHelper.leftPos(screen);
        int topPos = OverlayHelper.topPos(screen);

        Slot hovered = null;
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !isTabSlot(slot)) continue;
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;

            if (targeted && CreativeTabPreview.matchesRemoval(removals, stack)) {
                OverlayHelper.slotBox(graphics, sx, sy, REMOVED_FILL, REMOVED_OUTLINE);
            } else if (targeted && isAdded(additions, stack)) {
                OverlayHelper.slotBox(graphics, sx, sy, ADDED_FILL, ADDED_OUTLINE);
            } else if (CreativeTabPreview.isPending(stack.getItem())) {
                OverlayHelper.slotBox(graphics, sx, sy, 0, PENDING_OUTLINE);
            }

            if (OverlayHelper.insideSlot(mouseX, mouseY, sx, sy)) hovered = slot;
        }

        OverlayHelper.banner(graphics, targeted, tabId == null ? "?" : tabId.toString(), bannerDetail(targeted));

        if (hovered != null) OverlayHelper.slotBox(graphics, leftPos + hovered.x, topPos + hovered.y, 0, HOVER_OUTLINE);
    }

    @Nullable
    public static ItemStack pickAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot slot = OverlayHelper.slotAt(screen, mouseX, mouseY);
        if (slot == null || !isTabSlot(slot) || slot.getItem().isEmpty()) return null;
        return slot.getItem();
    }

    private static String bannerDetail(boolean targeted) {
        if (!targeted) return "this modifier doesn't target the open tab";
        int selected = CreativeTabPreview.pendingCount();
        if (selected == 0) return "click items to select them";
        return StrUtils.plural(selected, "item") + " selected   -   click again to unselect";
    }

    private static boolean isTabSlot(Slot slot) {
        return !(slot.container instanceof Inventory);
    }

    private record AddedSet(Set<Item> items, boolean inverse) {
    }

    // resolved once per frame, the item suppliers can be expensive to run per slot
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
}
