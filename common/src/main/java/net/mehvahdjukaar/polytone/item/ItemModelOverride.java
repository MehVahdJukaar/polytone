package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;
import java.util.regex.Pattern;

public class ItemModelOverride {
    protected final Dynamic<?> lazyComponent;
    protected final Integer stackCount;
    protected final Pattern pattern;
    protected ResourceLocation model;
    protected DataComponentMap decodedComponents;

    Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("components").forGetter(o -> o.lazyComponent),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.pattern()))
    ).apply(instance, ItemModelOverride::new));

    public ItemModelOverride(Dynamic<?> lazyComponent, ResourceLocation model, Optional<Integer> stackCount, Optional<Pattern> pattern) {
        this.lazyComponent = lazyComponent;
        this.model = model;
        this.stackCount = stackCount.orElse(null);
        this.pattern = pattern.orElse(null);
    }

    public ItemModelOverride(DataComponentMap map, ResourceLocation model) {
        this.lazyComponent = null;
        this.model = model;
        this.stackCount = null;
        this.pattern = null;
        this.decodedComponents = map;
    }


    public DataComponentMap getComponents() {
        if (this.decodedComponents == null) {
            this.decodedComponents = runCodec(this.lazyComponent);
        }
        return this.decodedComponents;
    }

    private static <T> DataComponentMap runCodec(Dynamic<T> dynamic) {
        var ra = Minecraft.getInstance().level.registryAccess();
        DynamicOps<T> ops = RegistryOps.create(dynamic.getOps(), ra);
        return DataComponentMap.CODEC.decode(ops, dynamic.cast(ops))
                .result().orElseThrow(() -> new JsonParseException("Failed to decode components map"))
                .getFirst();
    }

    public ResourceLocation model() {
        return this.model;
    }

    public Integer stackCount() {
        return this.stackCount;
    }

    public Pattern pattern() {
        return this.pattern;
    }


}
