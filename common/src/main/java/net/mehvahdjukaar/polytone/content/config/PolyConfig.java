package net.mehvahdjukaar.polytone.content.config;

import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function13;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class PolyConfig<T> implements OptionInstance.ValueSet<T> {

    private final Optional<String> valueTranslationKey;
    private final Map<String, T> presets;
    private final Map<String, T> sectionPresets;
    private final int displayOrder;
    private final Optional<String> section;
    private final Optional<Integer> sectionOrder;
    private final Optional<PerformanceImpact> performanceImpact;
    private final boolean wide;
    private final Map<String, TooltipImage> tooltipImages;
    private final T defaultValue;

    public static final Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "string", StringConfig.CODEC,
            "number", NumberConfig.CODEC,
            "bool", BoolConfig.CODEC));

    protected PolyConfig(Optional<String> valueTranslationKey, Map<String, T> presets,
                         Map<String, T> sectionPresets, int order, Optional<String> section,
                         Optional<Integer> sectionOrder, Optional<PerformanceImpact> performanceImpact,
                         boolean wide, Map<String, TooltipImage> tooltipImages, T defaultValue) {
        this.valueTranslationKey = valueTranslationKey;
        this.presets = presets;
        this.sectionPresets = sectionPresets;
        this.defaultValue = defaultValue;
        this.displayOrder = order;
        this.section = section;
        this.sectionOrder = sectionOrder;
        this.performanceImpact = performanceImpact;
        this.wide = wide;
        this.tooltipImages = tooltipImages;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Map<String, T> getPresets() {
        return presets;
    }

    /** Values for the entry's own section slider; {@code presets} feeds the pack-wide one. */
    public Map<String, T> getSectionPresets() {
        return sectionPresets;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Optional<String> getValueTranslationKey() {
        return valueTranslationKey;
    }

    /**
     * Optional grouping id. Entries sharing a section are listed together on the config screen
     * under a sub-header titled by the {@code config.<namespace>.section.<id>} lang key.
     */
    public Optional<String> getSection() {
        return section;
    }

    /**
     * Optional position of the entry's whole section in the tab; a section sorts by the smallest
     * section_order among its entries, else alphabetically. Entry order stays with display_order.
     */
    public Optional<Integer> getSectionOrder() {
        return sectionOrder;
    }

    /** Optional cost hint appended to the option tooltip as a colored "Performance Impact" line. */
    public Optional<PerformanceImpact> getPerformanceImpact() {
        return performanceImpact;
    }

    /** When true the option renders as a full-width row instead of a half-width paired button. */
    public boolean isWide() {
        return wide;
    }

    /** Per-value preview images shown in the option tooltip (keyed by the value's string form). */
    public Map<String, TooltipImage> getTooltipImages() {
        return tooltipImages;
    }

    /** A tooltip preview image: a resource-pack texture drawn at the given pixel size. */
    public record TooltipImage(Identifier texture, int width, int height) {
        public static final SchemaCodec<TooltipImage> CODEC = SchemaRecord.create(TooltipImage.class, i -> i.group(
                i.field("texture", Identifier.CODEC, TooltipImage::texture),
                i.field("width", Codec.INT, TooltipImage::width),
                i.field("height", Codec.INT, TooltipImage::height)
        ).apply(i, TooltipImage::new));
    }

    /**
     * Default value rendering for the options screen when no {@code value_translation} is given.
     * Each subclass owns its type's formatting so {@link OptionHolder} stays type-agnostic.
     */
    public abstract MutableComponent formatValue(T value);

    static <A, T extends PolyConfig<A>> @NonNull DataResult<T> validatePresets(T o) {
        //validate presets
        for (var map : List.of(o.getPresets(), o.getSectionPresets())) {
            for (var entry : map.entrySet()) {
                if (o.validateValue(entry.getValue()).isEmpty()) {
                    return DataResult.error(() -> "Preset value '" + entry.getValue() + "' for preset '" + entry.getKey() + "' is not valid");
                }
            }
        }
        return DataResult.success(o);
    }

    /** Extra (subclass-specific) field, deferred so it can be built on the codec's own Instance. */
    @FunctionalInterface
    interface FieldMaker<P, F> extends Function<SchemaRecord.Instance<P>, SchemaRecord.FieldRef<P, F>> {
    }

    /** Codec with exactly the 10 shared fields (BoolConfig-shaped), presets validated. */
    static <T, P extends PolyConfig<T>> SchemaCodec<P> commonCodec(
            Class<P> type, Codec<T> typeCodec,
            Function10<Optional<String>, Map<String, T>, Map<String, T>, Integer, Optional<String>,
                    Optional<Integer>, Optional<PerformanceImpact>, Boolean, Map<String, TooltipImage>, T, P> ctor) {
        return validated(SchemaRecord.create(type, i -> i.group(
                valueTranslationField(i),
                presetsField(i, typeCodec),
                sectionPresetsField(i, typeCodec),
                displayOrderField(i),
                sectionField(i),
                sectionOrderField(i),
                performanceImpactField(i),
                wideField(i),
                tooltipImagesField(i),
                defaultValueField(i, typeCodec)
        ).apply(i, ctor)));
    }

    /** The 10 shared fields plus one subclass extra (StringConfig-shaped), presets validated. */
    static <T, P extends PolyConfig<T>, E1> SchemaCodec<P> commonCodec(
            Class<P> type, Codec<T> typeCodec, FieldMaker<P, E1> extra1,
            Function11<Optional<String>, Map<String, T>, Map<String, T>, Integer, Optional<String>,
                    Optional<Integer>, Optional<PerformanceImpact>, Boolean, Map<String, TooltipImage>, T, E1, P> ctor) {
        return validated(SchemaRecord.create(type, i -> i.group(
                valueTranslationField(i),
                presetsField(i, typeCodec),
                sectionPresetsField(i, typeCodec),
                displayOrderField(i),
                sectionField(i),
                sectionOrderField(i),
                performanceImpactField(i),
                wideField(i),
                tooltipImagesField(i),
                defaultValueField(i, typeCodec),
                extra1.apply(i)
        ).apply(i, ctor)));
    }

    /** The 10 shared fields plus three subclass extras (NumberConfig-shaped), presets validated. */
    static <T, P extends PolyConfig<T>, E1, E2, E3> SchemaCodec<P> commonCodec(
            Class<P> type, Codec<T> typeCodec,
            FieldMaker<P, E1> extra1, FieldMaker<P, E2> extra2, FieldMaker<P, E3> extra3,
            Function13<Optional<String>, Map<String, T>, Map<String, T>, Integer, Optional<String>,
                    Optional<Integer>, Optional<PerformanceImpact>, Boolean, Map<String, TooltipImage>, T, E1, E2, E3, P> ctor) {
        return validated(SchemaRecord.create(type, i -> i.group(
                valueTranslationField(i),
                presetsField(i, typeCodec),
                sectionPresetsField(i, typeCodec),
                displayOrderField(i),
                sectionField(i),
                sectionOrderField(i),
                performanceImpactField(i),
                wideField(i),
                tooltipImagesField(i),
                defaultValueField(i, typeCodec),
                extra1.apply(i),
                extra2.apply(i),
                extra3.apply(i)
        ).apply(i, ctor)));
    }

    /** Wraps preset validation around the record codec while keeping its schema view. */
    private static <T, P extends PolyConfig<T>> SchemaCodec<P> validated(SchemaCodec<P> record) {
        return SchemaCodec.lazy(record.validate(PolyConfig::validatePresets), record::schema);
    }

    // Individual field builders (SchemaRecord FieldRefs, so the codec editor renders configs
    // structurally). Composed by commonCodec above; field order matches the shared prefix of
    // every subclass constructor.
    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Optional<String>> valueTranslationField(SchemaRecord.Instance<P> i) {
        return i.optional("value_translation", Codec.STRING, PolyConfig::getValueTranslationKey);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Map<String, T>> presetsField(SchemaRecord.Instance<P> i, Codec<T> typeCodec) {
        return i.optional("presets", Codec.unboundedMap(Codec.STRING, typeCodec), Map.of(), PolyConfig::getPresets);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Map<String, T>> sectionPresetsField(SchemaRecord.Instance<P> i, Codec<T> typeCodec) {
        return i.optional("section_presets", Codec.unboundedMap(Codec.STRING, typeCodec), Map.of(), PolyConfig::getSectionPresets);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Integer> displayOrderField(SchemaRecord.Instance<P> i) {
        return i.optional("display_order", Codec.INT, 0, PolyConfig::getDisplayOrder);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Optional<String>> sectionField(SchemaRecord.Instance<P> i) {
        return i.optional("section", Codec.STRING, PolyConfig::getSection);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Optional<Integer>> sectionOrderField(SchemaRecord.Instance<P> i) {
        return i.optional("section_order", Codec.INT, PolyConfig::getSectionOrder);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Optional<PerformanceImpact>> performanceImpactField(SchemaRecord.Instance<P> i) {
        return i.optional("performance_impact", PerformanceImpact.CODEC, PolyConfig::getPerformanceImpact);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Boolean> wideField(SchemaRecord.Instance<P> i) {
        return i.optional("wide", Codec.BOOL, false, PolyConfig::isWide);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, Map<String, TooltipImage>> tooltipImagesField(SchemaRecord.Instance<P> i) {
        return i.optional("tooltip_images", Codec.unboundedMap(Codec.STRING, TooltipImage.CODEC),
                Map.of(), PolyConfig::getTooltipImages);
    }

    static <T, P extends PolyConfig<T>> SchemaRecord.FieldRef<P, T> defaultValueField(SchemaRecord.Instance<P> i, Codec<T> typeCodec) {
        return i.field("default_value", typeCodec, PolyConfig::getDefaultValue);
    }

    // Mirrors Sodium's OptionImpact levels and colors so tooltips read consistently across mods.
    public enum PerformanceImpact implements StringRepresentable {
        LOW("low", ChatFormatting.GREEN),
        MEDIUM("medium", ChatFormatting.YELLOW),
        HIGH("high", ChatFormatting.GOLD),
        VARIES("varies", ChatFormatting.WHITE);

        public static final Codec<PerformanceImpact> CODEC = StringRepresentable.fromEnum(PerformanceImpact::values);

        private final String name;
        private final Component displayName;

        PerformanceImpact(String name, ChatFormatting color) {
            this.name = name;
            this.displayName = Component.translatable("polytone.performance_impact." + name).withStyle(color);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Component getDisplayName() {
            return displayName;
        }
    }
}
