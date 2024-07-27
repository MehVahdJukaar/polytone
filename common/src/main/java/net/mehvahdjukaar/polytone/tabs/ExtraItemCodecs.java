package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class ExtraItemCodecs {


    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.either(BuiltInRegistries.ITEM.byNameCodec(), ItemStack.SINGLE_ITEM_CODEC)
            .xmap(e -> e.map(Item::getDefaultInstance, Function.identity()), Either::right);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.either(ITEM_OR_STACK, ITEM_OR_STACK.listOf())
            .xmap(e -> e.map(List::of, Function.identity()), Either::right);

    public static final Codec<LazyHolderSet<Item>> ITEM_SET = LazyHolderSet.codec(Registries.ITEM);
    public static final Codec<List<ItemStack>> ITEMSTACK_SET = Codec.either(
            ITEMSTACK_OR_ITEMSTACK_LIST, ITEM_SET).xmap(e -> e.map(Function.identity(),
            l -> l.stream().map(i -> i.value().getDefaultInstance()).toList()), Either::left);
}
