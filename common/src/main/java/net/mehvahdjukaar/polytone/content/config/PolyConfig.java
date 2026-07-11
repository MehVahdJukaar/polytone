package net.mehvahdjukaar.polytone.content.config;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
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
        public static final Codec<TooltipImage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("texture").forGetter(TooltipImage::texture),
                Codec.INT.fieldOf("width").forGetter(TooltipImage::width),
                Codec.INT.fieldOf("height").forGetter(TooltipImage::height)
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

    // Individual field builders. Subclasses with many extra fields compose these in one flat
    // group() call since Products.and() chaining caps out at 8 total fields.
    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Optional<String>> valueTranslationField() {
        return Codec.STRING.optionalFieldOf("value_translation").forGetter(PolyConfig::getValueTranslationKey);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Map<String, T>> presetsField(Codec<T> typeCodec) {
        return Codec.unboundedMap(Codec.STRING, typeCodec).optionalFieldOf("presets", Map.of()).forGetter(PolyConfig::getPresets);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Map<String, T>> sectionPresetsField(Codec<T> typeCodec) {
        return Codec.unboundedMap(Codec.STRING, typeCodec).optionalFieldOf("section_presets", Map.of()).forGetter(PolyConfig::getSectionPresets);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Integer> displayOrderField() {
        return Codec.INT.optionalFieldOf("display_order", 0).forGetter(PolyConfig::getDisplayOrder);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Optional<String>> sectionField() {
        return Codec.STRING.optionalFieldOf("section").forGetter(PolyConfig::getSection);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Optional<Integer>> sectionOrderField() {
        return Codec.INT.optionalFieldOf("section_order").forGetter(PolyConfig::getSectionOrder);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Optional<PerformanceImpact>> performanceImpactField() {
        return PerformanceImpact.CODEC.optionalFieldOf("performance_impact").forGetter(PolyConfig::getPerformanceImpact);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Boolean> wideField() {
        return Codec.BOOL.optionalFieldOf("wide", false).forGetter(PolyConfig::isWide);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, Map<String, TooltipImage>> tooltipImagesField() {
        return Codec.unboundedMap(Codec.STRING, TooltipImage.CODEC)
                .optionalFieldOf("tooltip_images", Map.of()).forGetter(PolyConfig::getTooltipImages);
    }

    static <T, P extends PolyConfig<T>> RecordCodecBuilder<P, T> defaultValueField(Codec<T> typeCodec) {
        return typeCodec.fieldOf("default_value").forGetter(PolyConfig::getDefaultValue);
    }

    static <T, P extends PolyConfig<T>> Products.P10<RecordCodecBuilder.Mu<P>, Optional<String>, Map<String, T>, Map<String, T>, Integer, Optional<String>, Optional<Integer>, Optional<PerformanceImpact>, Boolean, Map<String, TooltipImage>, T> commonFields(
            RecordCodecBuilder.Instance<P> instance, Codec<T> typeCodec) {
        return instance.group(
                valueTranslationField(),
                presetsField(typeCodec),
                sectionPresetsField(typeCodec),
                displayOrderField(),
                sectionField(),
                sectionOrderField(),
                performanceImpactField(),
                wideField(),
                tooltipImagesField(),
                defaultValueField(typeCodec));
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
