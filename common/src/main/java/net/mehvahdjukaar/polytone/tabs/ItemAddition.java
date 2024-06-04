package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemAddition(List<ItemStack> stack, ItemPredicate predicate, boolean before) {



    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraItemCodecs.ITEMSTACK_OR_ITEMSTACK_LIST
                    .fieldOf("items").forGetter(ItemAddition::stack),
            StrOpt.of(ItemPredicate.CODEC, "predicate", ItemPredicate.TRUE_PRED).forGetter(ItemAddition::predicate),
            StrOpt.of(Codec.BOOL, "before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));

}
