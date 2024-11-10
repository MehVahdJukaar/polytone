package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.utils.ModelResHelper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class StandaloneItemModelOverride extends ItemModelOverride {


    public static final Codec<StandaloneItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.fieldOf("components").forGetter(i -> i.lazyComponent),
            ModelResHelper.MODEL_RES_CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.namePattern())),
            CompoundTag.CODEC.optionalFieldOf("entity_tag").forGetter(i -> Optional.ofNullable(i.entityTag)),
            ColormapExpressionProvider.CODEC.optionalFieldOf("expression").forGetter(i -> Optional.ofNullable(i.expression)),
            NBT_COMPONENTS_CODEC.optionalFieldOf("item_nbt_components", Map.of()).forGetter(i -> i.nbtMatchers),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(StandaloneItemModelOverride::getTarget)
    ).apply(instance, StandaloneItemModelOverride::new));

    private final Item item;
    private final boolean autoModel;

    public StandaloneItemModelOverride(Dynamic<?> components, ModelResourceLocation model,
                                       Optional<Integer> stackCount, Optional<Pattern> pattern,
                                       Optional<CompoundTag> entityTag, Optional<ColormapExpressionProvider> expression,
                                       Map<DataComponentType<?>, CompoundTag> nbtMatchers,
                                       Item target) {
        super(components, model, stackCount, pattern, entityTag, expression, nbtMatchers);
        this.item = target;
        this.autoModel = model.toString().equals("minecraft:generated");
    }

    // ugly
    public void setModel(ModelResourceLocation model) {
        this.model = model;
    }

    public Item getTarget() {
        return item;
    }

    public boolean isAutoModel() {
        return autoModel;
    }
}
