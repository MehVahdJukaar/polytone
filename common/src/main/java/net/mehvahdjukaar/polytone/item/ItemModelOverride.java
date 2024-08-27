package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class ItemModelOverride {

    public static final Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataComponentMap.CODEC.fieldOf("components").forGetter(ItemModelOverride::components),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model)
    ).apply(instance, ItemModelOverride::new));

    private final DataComponentMap components;
    private final ResourceLocation model;

    public ItemModelOverride(DataComponentMap components, ResourceLocation model) {
        this.components = components;
        this.model = model;
    }


    public BakedModel fetchModel() {
        return PlatStuff.getBakedModel(this.model);
    }

    public TypedDataComponent<?>[] getComponents() {
        return this.components.stream().toArray(TypedDataComponent<?>[]::new);
    }

    public DataComponentMap components() {
        return components;
    }

    public ResourceLocation model() {
        return model;
    }

}
