package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface ItemModelOverride {

    Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataComponentMap.CODEC.fieldOf("components").forGetter(ItemModelOverride::components),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount()))
    ).apply(instance, ItemModelOverride::of));

    static ItemModelOverride of(DataComponentMap staff, ResourceLocation staff1, Optional<Integer> stackCount) {
        return new Impl(staff, staff1, stackCount.orElse(null));
    }

    static ItemModelOverride of(DataComponentMap staff, ResourceLocation staff1) {
        return new Impl(staff, staff1, null);
    }


    record Impl(DataComponentMap components, ResourceLocation model, Integer stackCount) implements ItemModelOverride {
    }

    DataComponentMap components();

    ResourceLocation model();

    default boolean hasStackCount() {
        return stackCount() != null;
    }

    Integer stackCount();

}
