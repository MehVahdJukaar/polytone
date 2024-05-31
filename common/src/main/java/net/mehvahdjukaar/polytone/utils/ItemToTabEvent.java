package net.mehvahdjukaar.polytone.utils;


import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public record ItemToTabEvent(
        QuadConsumer<ResourceKey<CreativeModeTab>, @Nullable Predicate<ItemStack>, Boolean, Collection<ItemStack>> action) {


    public void add(ResourceKey<CreativeModeTab> tab, ItemLike... items) {
        addAfter(tab, null, items);
    }

    public void add(ResourceKey<CreativeModeTab> tab, ItemStack... items) {
        addAfter(tab, null, items);
    }

    public void addAfter(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();

        for (var i : items) {
            if (i.asItem().getDefaultInstance().isEmpty()) {
                throw new IllegalStateException("Attempted to add empty item " + i + " to item tabs");
            } else stacks.add(i.asItem().getDefaultInstance());
        }
        action.accept(tab, target, true, stacks);
    }

    public void addAfter(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemStack... items) {
        action.accept(tab, target, true, java.util.List.of(items));
    }

    public void addBefore(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var i : items) {
            if (i.asItem().getDefaultInstance().isEmpty()) {
                throw new IllegalStateException("Attempted to add empty item " + i + " to item tabs");
            } else stacks.add(i.asItem().getDefaultInstance());
        }
        action.accept(tab, target, false, stacks);
    }

    public void addBefore(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemStack... items) {
        action.accept(tab, target, false, java.util.List.of(items));
    }
}

