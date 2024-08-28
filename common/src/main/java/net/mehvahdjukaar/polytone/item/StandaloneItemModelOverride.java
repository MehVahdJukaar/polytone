package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class StandaloneItemModelOverride implements ItemModelOverride {


    public static final Codec<StandaloneItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataComponentMap.CODEC.fieldOf("components").forGetter(ItemModelOverride::components),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(StandaloneItemModelOverride::getTarget)
    ).apply(instance, StandaloneItemModelOverride::new));

    private final DataComponentMap components;
    private final Item item;
    private final boolean autoModel;
    private ResourceLocation model;

    public StandaloneItemModelOverride(DataComponentMap components, ResourceLocation model, Item target) {
        this.components = components;
        this.item = target;
        this.model = model;
        this.autoModel = model.toString().equals("minecraft:generated");
    }

    // ugly
    public void setModel(ResourceLocation model) {
        this.model = model;
    }

    public Item getTarget() {
        return item;
    }

    public boolean isAutoModel() {
        return autoModel;
    }

    @Override
    public DataComponentMap components() {
        return components;
    }

    @Override
    public ResourceLocation model() {
        return model;
    }
}
