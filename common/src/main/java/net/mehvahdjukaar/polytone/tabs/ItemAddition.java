package net.mehvahdjukaar.polytone.tabs;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ItemAddition(Dynamic<?> dynamicStacks, boolean inverse, ItemPredicate predicate, boolean before) {

    public static final Codec<ItemAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("items").forGetter(ItemAddition::dynamicStacks),
            Codec.BOOL.optionalFieldOf("inverse", false).forGetter(ItemAddition::inverse),
            ItemPredicate.CODEC.optionalFieldOf("predicate", net.mehvahdjukaar.polytone.tabs.ItemPredicate.TRUE_PRED).forGetter(ItemAddition::predicate),
            Codec.BOOL.optionalFieldOf("before", false).forGetter(ItemAddition::before)
    ).apply(instance, ItemAddition::new));


    @Nullable
    public List<ItemStack> getItems(RegistryAccess access) {
        var res = genericStuff(dynamicStacks, access);
        if (res.result().isPresent()) {
            return res.result().get().getFirst();
        }
        Polytone.LOGGER.error("Failed to decode item addition: {}", res.error());
        return null;
    }

    private static <T> DataResult<Pair<List<ItemStack>, T>> genericStuff(Dynamic<T> d, RegistryAccess access) {
        return ExtraItemCodecs.ITEMSTACK_SET.decode(RegistryOps.create(d.getOps(), access), d.getValue());
    }
}
