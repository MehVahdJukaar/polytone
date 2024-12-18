package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.CodecUtil;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemAddition(List<ItemStack> items, boolean inverse, ItemPredicate predicate, boolean before) {

    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.ITEMSTACK_OR_ITEMSTACK_LIST.fieldOf("items").forGetter(ItemAddition::items),
            StrOpt.of(Codec.BOOL, "inverse", false).forGetter(ItemAddition::inverse),
            StrOpt.of(ItemPredicate.CODEC, "predicate", net.mehvahdjukaar.polytone.tabs.ItemPredicate.TRUE_PRED).forGetter(ItemAddition::predicate),
            StrOpt.of(Codec.BOOL, "before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));


}
