package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;

public interface ItemModelOverride {

    Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataComponentMap.CODEC.fieldOf("components").forGetter(ItemModelOverride::components),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model)
    ).apply(instance, ItemModelOverride.Impl::new));

    static ItemModelOverride of(DataComponentMap staff, ResourceLocation staff1) {
        return new Impl(staff, staff1);
    }


    record Impl(DataComponentMap components, ResourceLocation model) implements ItemModelOverride {
    }

    DataComponentMap components();

    ResourceLocation model();

}
