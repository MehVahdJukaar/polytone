package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record ItemAddition(Supplier<List<ItemStack>> items, boolean inverse, ItemPredicate predicate, boolean before) {

    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.ITEMSTACK_OR_LIST_OR_HOLDER_SET.fieldOf("items").forGetter(ItemAddition::items),
            StrOpt.of(Codec.BOOL, "inverse", false).forGetter(ItemAddition::inverse),
            StrOpt.of(ItemPredicate.CODEC, "predicate", ItemPredicate.TRUE_PRED).forGetter(ItemAddition::predicate),
            StrOpt.of(Codec.BOOL, "before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));


}
