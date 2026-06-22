package net.mehvahdjukaar.polytone.content.tabs;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@link ItemToTabEvent} implementation that operates directly on a creative tab's final item
 * collections, instead of being part of a loader event. It is invoked from
 * {@code CreativeModeTabMixin} at the tail of {@link CreativeModeTab#buildContents}, which is
 * guaranteed to run after every other mod's NeoForge/Fabric tab event. This way Polytone always
 * reorders/modifies tab contents last, once all other items have been added.
 */
public record TabContentsEvent(ResourceKey<CreativeModeTab> tab,
                               Collection<ItemStack> displayItems,
                               Collection<ItemStack> searchItems) implements ItemToTabEvent {

    @Override
    public ResourceKey<CreativeModeTab> getTab() {
        return tab;
    }

    @Override
    public Collection<ItemStack> getAllItems() {
        return displayItems;
    }

    @Override
    public void removeItems(Predicate<ItemStack> target) {
        displayItems.removeIf(target);
        searchItems.removeIf(target);
    }

    @Override
    public void addItems(@Nullable Predicate<ItemStack> target, boolean after, List<ItemStack> items) {
        if (items.isEmpty()) return;
        boolean append = target == null || target == ItemPredicate.TRUE_PRED;
        insertInto(displayItems, target, after, items, append);
        insertInto(searchItems, target, after, items, append);
    }

    // The tab collections are insertion-ordered sets, so we rebuild them to place items at a
    // specific position relative to a target item.
    private static void insertInto(Collection<ItemStack> coll, @Nullable Predicate<ItemStack> target,
                                   boolean after, List<ItemStack> items, boolean append) {
        if (append || coll.isEmpty()) {
            coll.addAll(items);
            return;
        }
        List<ItemStack> list = new ArrayList<>(coll);
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (target.test(list.get(i))) {
                index = i;
                if (!after) break; // first match when inserting "before"; last match when "after"
            }
        }
        if (index < 0) return; // target not present in this collection, leave it untouched
        list.addAll(after ? index + 1 : index, items);
        coll.clear();
        coll.addAll(list);
    }
}
