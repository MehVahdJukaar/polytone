package net.mehvahdjukaar.polytone.content.tabs;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record ItemAddition(Supplier<List<ItemStack>> items, boolean inverse, ItemPredicate predicate, boolean before) {

    public static final SchemaCodec<ItemAddition> CODEC = SchemaRecord.create(ItemAddition.class, i -> i.group(
            i.field("items", SchemaCodecs.ITEMSTACK_OR_LIST_OR_HOLDER_SET, ItemAddition::items),
            i.optional("inverse", Codec.BOOL, false, ItemAddition::inverse),
            i.optional("predicate", ItemPredicate.CODEC, ItemPredicate.TRUE_PRED, ItemAddition::predicate),
            i.optional("before", Codec.BOOL, false, ItemAddition::before)
    ).apply(i, ItemAddition::new));


}
