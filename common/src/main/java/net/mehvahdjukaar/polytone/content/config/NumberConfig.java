package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ExtraCodecs;

import java.math.BigDecimal;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class NumberConfig extends PolyConfig<Float> implements OptionInstance.SliderableValueSet<Float> {

    public static final SchemaCodec<NumberConfig> CODEC = PolyConfig.validated(
            SchemaRecord.create(NumberConfig.class, i -> PolyConfig.commonFields(i, Codec.FLOAT)
                    .and(SchemaRecord.optional("min", Codec.FLOAT, 0f, c -> c.min))
                    .and(SchemaRecord.optional("max", Codec.FLOAT, 1f, c -> c.max))
                    .and(SchemaRecord.optional("step", ExtraCodecs.POSITIVE_FLOAT, 0.1f, c -> c.step))
                    .apply(i, NumberConfig::new)));

    private final float step;
    private final float min;
    private final float max;
    private final int decimals;

    protected NumberConfig(Optional<String> valueTranslation, Map<String, Float> presets,
                           Map<String, Float> sectionPresets, int order,
                           Optional<String> section, Optional<Integer> sectionOrder,
                           Optional<PerformanceImpact> performanceImpact,
                           boolean wide, Map<String, TooltipImage> tooltipImages,
                           float defaultValue, float min, float max, float step) {
        super(valueTranslation, presets, sectionPresets, order, section, sectionOrder,
                performanceImpact, wide, tooltipImages, defaultValue);
        this.step = step;
        this.min = min;
        this.max = max;
        this.decimals = Math.max(0, new BigDecimal(Float.toString(step)).stripTrailingZeros().scale());
    }

    @Override
    public double toSliderValue(Float object) {
        //slider value is always normalized between 0 and 1
        return (object - min) / (max - min);
    }

    @Override
    public Float fromSliderValue(double d) {
        float v = (float) (min + (max - min) * d);
        //snap to step
        if (step > 0) {
            v = Math.round(v / step) * step;
        }
        return v;
    }

    @Override
    public Optional<Float> validateValue(Float object) {
        if (object < min || object > max) return Optional.empty();
        return Optional.of(object);
    }

    @Override
    public Codec<Float> codec() {
        return Codec.FLOAT;
    }

    @Override
    public MutableComponent formatValue(Float value) {
        return Component.literal(String.format(Locale.ROOT, "%." + decimals + "f", value));
    }

    @Override
    public Optional<Float> next(Float object) {
        float next = object + step;
        if (next > max) return Optional.empty();
        return Optional.of(next);
    }

    @Override
    public Optional<Float> previous(Float object) {
        float prev = object - step;
        if (prev < min) return Optional.empty();
        return Optional.of(prev);
    }


    @Override
    public Function<OptionInstance<Float>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<Float> tooltipSupplier, Options options, int i, int j, int k, OptionInstance.ValueUpdateListener<? super Float> consumer) {
        return OptionInstance.SliderableValueSet.super.createButton(tooltipSupplier, options, i, j, k, consumer);
    }
}
