package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ExtraItemCodecs {

    // with no mandatory count
    public static final Codec<ItemStack> ITEMSTACK = RecordCodecBuilder.create((i) -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ItemStack::getItem),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter((s) -> Optional.ofNullable(s.getTag()))
    ).apply(i, (item, tag) -> {
        var stack = new ItemStack(item, 1);
        tag.ifPresent(stack::setTag);
        return stack;
    }));

    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.either(BuiltInRegistries.ITEM.byNameCodec(), ITEMSTACK)
            .xmap(e -> e.map(Item::getDefaultInstance, Function.identity()), Either::right);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.either(ITEM_OR_STACK, ITEM_OR_STACK.listOf())
            .xmap(e -> e.map(List::of, Function.identity()), Either::right);

    public static final Codec<HolderSet<Item>> ITEM_SET = RegistryCodecs.homogeneousList(Registries.ITEM, true);
    public static final Codec<List<ItemStack>> ITEMSTACK_SET = Codec.either(
            ITEMSTACK_OR_ITEMSTACK_LIST,ITEM_SET ).xmap(e -> e.map(Function.identity(),
            l -> l.stream().map(i -> i.value().getDefaultInstance()).toList()), Either::left);
}
