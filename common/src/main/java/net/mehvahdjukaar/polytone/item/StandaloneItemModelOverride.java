package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.regex.Pattern;

public class StandaloneItemModelOverride extends ItemModelOverride {


    public static final Codec<StandaloneItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("components").forGetter(i -> i.lazyComponent),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.pattern())),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(StandaloneItemModelOverride::getTarget)
    ).apply(instance, StandaloneItemModelOverride::new));

    private final Item item;
    private final boolean autoModel;

    public StandaloneItemModelOverride(Dynamic<?> components, ResourceLocation model,
                                       Optional<Integer> stackCount, Optional<Pattern> pattern, Item target) {
        super(components, model, stackCount, pattern);
        this.item = target;
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
}
