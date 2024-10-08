package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class ExtraItemCodecs {


    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, BuiltInRegistries.ITEM.byNameCodec(),
            Item::getDefaultInstance);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.withAlternative(ITEM_OR_STACK.listOf(), ITEM_OR_STACK,
            List::of);
}
