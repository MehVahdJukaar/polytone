package net.mehvahdjukaar.polytone.content.item;

public class StandaloneItemModelOverride extends ItemModelOverride {
/*

    public static final Codec<StandaloneItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.PASSTHROUGH.optionalFieldOf("components").forGetter(i -> Optional.ofNullable(i.lazyComponent)),
            ModelResHelper.MODEL_RES_CODEC.fieldOf("model").forGetter(ItemModelOverride::model),
            Codec.INT.optionalFieldOf("stack_count").forGetter(i -> Optional.ofNullable(i.stackCount())),
            ExtraCodecs.PATTERN.optionalFieldOf("name_pattern").forGetter(i -> Optional.ofNullable(i.namePattern())),
            CompoundTag.CODEC.optionalFieldOf("entity_tag").forGetter(i -> Optional.ofNullable(i.entityTag)),
            ColormapExpressionProvider.CODEC.optionalFieldOf("expression").forGetter(i -> Optional.ofNullable(i.expression)),
            NBT_COMPONENTS_CODEC.optionalFieldOf("item_nbt_components", Map.of()).forGetter(i -> i.nbtMatchers),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(StandaloneItemModelOverride::getTarget)
    ).apply(instance, StandaloneItemModelOverride::new));

    public static final Codec<Partial> CODEC_MODEL_ONLY = RecordCodecBuilder.create(instance -> instance.group(
            ModelResHelper.MODEL_RES_CODEC.fieldOf("model").forGetter(Partial::model)
    ).apply(instance, Partial::new));

    public record Partial(Mode model, boolean autoModel) {
        public Partial(ModelResourceLocation model) {
            this(model, model.toString().equals("minecraft:generated"));
        }
    }

    private final Item item;
    private final boolean autoModel;

    public StandaloneItemModelOverride(Optional<Dynamic<?>> components, ModelResourceLocation model,
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
    }*/
}
