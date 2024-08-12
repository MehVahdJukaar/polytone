package net.mehvahdjukaar.polytone.tabs;


import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface ItemToTabEvent {

    ResourceKey<CreativeModeTab> getTab();

    void addItems(@Nullable Predicate<ItemStack> target, boolean after, List<ItemStack> items);

    void removeItems(Predicate<ItemStack> target);

    default void add(ItemLike... items) {
        addAfter(null, items);
    }

    default void add(ItemStack... items) {
        addAfter(null, items);
    }

    default void addAfter(Predicate<ItemStack> target, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();

        for (var i : items) {
            if (i.asItem().getDefaultInstance().isEmpty()) {
                throw new IllegalStateException("Attempted to add empty item " + i + " to item tabs");
            } else stacks.add(i.asItem().getDefaultInstance());
        }
        addItems(target, true, stacks);
    }

    default void addAfter(Predicate<ItemStack> target, ItemStack... items) {
        addItems(target, true, java.util.List.of(items));
    }

    default void addBefore(Predicate<ItemStack> target, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var i : items) {
            if (i.asItem().getDefaultInstance().isEmpty()) {
                throw new IllegalStateException("Attempted to add empty item " + i + " to item tabs");
            } else stacks.add(i.asItem().getDefaultInstance());
        }
        addItems(target, false, stacks);
    }

    default void addBefore(Predicate<ItemStack> target, ItemStack... items) {
        addItems(target, false, java.util.List.of(items));
    }


}

