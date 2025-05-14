package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class ExtraItemCodecs {


    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, BuiltInRegistries.ITEM.byNameCodec(),
            Item::getDefaultInstance);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.withAlternative(ITEM_OR_STACK.listOf(), ITEM_OR_STACK,
            List::of);

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_HOLDER_SET = RegistryCodecs.homogeneousList(Registries.ITEM)
            .xmap(l -> () -> l.stream().map(Holder::value).map(ItemStack::new).toList(), s -> HolderSet.direct(s.get().stream().map(ItemStack::getItemHolder).toList()));

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_OR_LIST_OR_HOLDER_SET =
            Codec.withAlternative(
                    ITEMSTACK_OR_ITEMSTACK_LIST.xmap(l -> () -> l, Supplier::get),
                    ITEMSTACK_HOLDER_SET);

}
