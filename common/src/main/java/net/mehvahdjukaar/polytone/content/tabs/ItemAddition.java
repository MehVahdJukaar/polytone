package net.mehvahdjukaar.polytone.content.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record ItemAddition(Supplier<List<ItemStack>> items, boolean inverse, ItemPredicate predicate, boolean before) {

    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SchemaCodecs.ITEMSTACK_OR_LIST_OR_HOLDER_SET.fieldOf("items").forGetter(ItemAddition::items),
            Codec.BOOL.optionalFieldOf("inverse", false).forGetter(ItemAddition::inverse),
            ItemPredicate.CODEC.optionalFieldOf("predicate", ItemPredicate.TRUE_PRED).forGetter(ItemAddition::predicate),
            Codec.BOOL.optionalFieldOf("before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));


}
