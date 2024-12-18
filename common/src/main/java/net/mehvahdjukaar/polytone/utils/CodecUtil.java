package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CodecUtil {
    public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<? extends T> alternative) {
        return Codec.either(
                primary,
                alternative
        ).xmap(
                e -> e.map(Function.identity(), Function.identity()),
                Either::left
        );
    }

    public static <T, U> Codec<T> withAlternative(final Codec<T> primary, final Codec<U> alternative, final Function<U, T> converter) {
        return Codec.either(
                primary,
                alternative
        ).xmap(
                either -> either.map(v -> v, converter),
                Either::left
        );
    }



    public static final Codec<Item> NONNULL_ITEM = BuiltInRegistries.ITEM.holderByNameCodec()
            .xmap(Holder::value, BuiltInRegistries.ITEM::wrapAsHolder);

    // with no mandatory count
    public static final Codec<ItemStack> ITEMSTACK = RecordCodecBuilder.create((i) -> i.group(
            NONNULL_ITEM.fieldOf("id").forGetter(ItemStack::getItem),
            StrOpt.of(CompoundTag.CODEC, "tag").forGetter((s) -> Optional.ofNullable(s.getTag()))
    ).apply(i, (item, tag) -> {
        var stack = new ItemStack(item, 1);
        tag.ifPresent(stack::setTag);
        return stack;
    }));

    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.either(NONNULL_ITEM, ITEMSTACK)
            .xmap(e -> e.map(Item::getDefaultInstance, Function.identity()), Either::right);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.either(ITEM_OR_STACK, ITEM_OR_STACK.listOf())
            .xmap(e -> e.map(List::of, Function.identity()), Either::right);
}
