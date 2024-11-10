package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.mehvahdjukaar.polytone.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ModelResHelper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
    protected ModelResourceLocation model;
    protected DataComponentMap decodedComponents;
    protected Map<DataComponentType<?>, CompoundTag> nbtMatchers;

    protected static final Codec<Map<ResourceLocation, Float>> ITEM_PREDICATE_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT);

    protected static final UnboundedMapCodec<DataComponentType<?>, CompoundTag> NBT_COMPONENTS_CODEC = Codec.unboundedMap(DataComponentType.CODEC, CompoundTag.CODEC);

    public static final Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("components").forGetter(o -> o.lazyComponent),
            ModelResHelper.MODEL_RES_CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.namePattern())),
            CompoundTag.CODEC.optionalFieldOf("entity_nbt").forGetter(i -> Optional.ofNullable(i.entityTag)),
            ColormapExpressionProvider.CODEC.optionalFieldOf("expression").forGetter(i -> Optional.ofNullable(i.expression)),
            NBT_COMPONENTS_CODEC.optionalFieldOf("item_nbt_components", Map.of()).forGetter(i -> i.nbtMatchers)
    ).apply(instance, ItemModelOverride::new));

    public ItemModelOverride(Dynamic<?> lazyComponent, ModelResourceLocation model, Optional<Integer> stackCount,
                             Optional<Pattern> pattern, Optional<CompoundTag> entityTag,
                             Optional<ColormapExpressionProvider> expression,
                             Map<DataComponentType<?>,CompoundTag> nbtMatchers) {
        this.lazyComponent = lazyComponent;
        this.model = model;
        this.stackCount = stackCount.orElse(null);
        this.pattern = pattern.orElse(null);
        this.entityTag = entityTag.orElse(null);
        this.expression = expression.orElse(null);
        this.nbtMatchers = nbtMatchers;
    }

    public ItemModelOverride(DataComponentMap map, ModelResourceLocation model) {
        this.lazyComponent = null;
        this.model = model;
        this.stackCount = null;
        this.pattern = null;
        this.decodedComponents = map;
        this.entityTag = null;
        this.expression = null;
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

    public ModelResourceLocation model() {
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

        for (var m : this.nbtMatchers.entrySet()) {
            var type = m.getKey();
            var c = stack.get(type);
            if (c instanceof CustomData d) {
                if (!containsTag(d.getUnsafe(), m.getValue())) return false;
            }
        }

        return true;
    }

    private static boolean containsTag(CompoundTag tagToMatch, CompoundTag entityTag) {
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
