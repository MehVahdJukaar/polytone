package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class ItemModelOverride {
    @Nullable
    protected final Dynamic<?> lazyComponent;
    @Nullable
    protected final Integer stackCount;
    @Nullable
    protected final Pattern pattern;
    @Nullable
    protected final CompoundTag entityTag;
    @Nullable
    protected final ColormapExpressionProvider expression;
    protected final Map<ResourceLocation, Float> predicates; //TODO: add or remove
    protected ResourceLocation model;
    protected DataComponentMap decodedComponents;

    protected static final Codec<Map<ResourceLocation, Float>> ITEM_PREDICATE_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT);

    public static final Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("components").forGetter(o -> o.lazyComponent),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.namePattern())),
            CompoundTag.CODEC.optionalFieldOf("entity_tag").forGetter(i -> Optional.ofNullable(i.entityTag)),
            ColormapExpressionProvider.CODEC.optionalFieldOf("expression").forGetter(i -> Optional.ofNullable(i.expression)),
            ITEM_PREDICATE_CODEC.optionalFieldOf("predicates", Map.of()).forGetter(i -> i.predicates)
    ).apply(instance, ItemModelOverride::new));

    public ItemModelOverride(Dynamic<?> lazyComponent, ResourceLocation model, Optional<Integer> stackCount,
                             Optional<Pattern> pattern, Optional<CompoundTag> entityTag,
                             Optional<ColormapExpressionProvider> expression, Map<ResourceLocation, Float> predicates) {
        this.lazyComponent = lazyComponent;
        this.model = model;
        this.stackCount = stackCount.orElse(null);
        this.pattern = pattern.orElse(null);
        this.entityTag = entityTag.orElse(null);
        this.expression = expression.orElse(null);
        this.predicates = predicates;
    }

    public ItemModelOverride(DataComponentMap map, ResourceLocation model) {
        this.lazyComponent = null;
        this.model = model;
        this.stackCount = null;
        this.pattern = null;
        this.decodedComponents = map;
        this.entityTag = null;
        this.expression = null;
        this.predicates = Map.of();
    }


    public DataComponentMap getComponents(RegistryAccess registryAccess) {
        if (this.decodedComponents == null && this.lazyComponent != null) {
            this.decodedComponents = runCodec(registryAccess, this.lazyComponent);
        }
        return this.decodedComponents;
    }

    private static <T> DataComponentMap runCodec(RegistryAccess ra, Dynamic<T> dynamic) {
        DynamicOps<T> ops = RegistryOps.create(dynamic.getOps(), ra);
        return DataComponentMap.CODEC.decode(ops, dynamic.getValue())
                .result().orElseThrow(() -> new JsonParseException("Failed to decode components map"))
                .getFirst();
    }

    public ResourceLocation model() {
        return this.model;
    }

    @Nullable
    public Integer stackCount() {
        return this.stackCount;
    }

    @Nullable
    public Pattern namePattern() {
        return this.pattern;
    }

    @Nullable
    public CompoundTag entityTag() {
        return this.entityTag;
    }


    public boolean matchesPredicate(ItemStack stack, @Nullable Level level, @Nullable Supplier<CompoundTag> entityTagGetter,
                                    @Nullable Component customName) {
        if (this.pattern != null && customName != null) {
            if (!this.pattern.matcher(customName.getString()).matches()) return false;
        }
        if (this.entityTag != null && entityTagGetter != null) {
            CompoundTag tag = entityTagGetter.get();
            if (!containsTag(tag, this.entityTag)) return false;
        }
        if (this.expression != null) {
            BlockPos pos = ClientFrameTicker.getCameraPos();
            if (this.expression.getValue(null, pos, null, null, stack) == 0) return false;
        }

        return true;
    }

    private boolean containsTag(CompoundTag tagToMatch, CompoundTag entityTag) {
        for (String key : tagToMatch.getAllKeys()) {
            Tag t = entityTag.get(key);
            if (t == null) return false;
            if (t instanceof CompoundTag ct) {
                CompoundTag compound = tagToMatch.getCompound(key);
                if (compound.isEmpty()) return false;
                if (!containsTag(compound, ct)) return false;
            } else if (!t.equals(entityTag.get(key))) return false;
        }
        return true;
    }

}
