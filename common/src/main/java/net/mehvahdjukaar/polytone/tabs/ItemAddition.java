package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public record ItemAddition(List<ItemStack> stack, ItemPredicate predicate, boolean before) {

    private static final Codec<ItemStack> ITEM_OR_STACK = Codec.either(BuiltInRegistries.ITEM.byNameCodec(), ItemStack.CODEC)
            .xmap(e -> e.map(Item::getDefaultInstance, Function.identity()), Either::right);

    private static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.either(ITEM_OR_STACK, ITEM_OR_STACK.listOf())
            .xmap(e -> e.map(List::of, Function.identity()), Either::right);

    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEMSTACK_OR_ITEMSTACK_LIST
                    .fieldOf("items").forGetter(ItemAddition::stack),
            StrOpt.of(ItemPredicate.CODEC, "predicate", ItemPredicate.TRUE).forGetter(ItemAddition::predicate),
            StrOpt.of(Codec.BOOL, "before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));

    }
