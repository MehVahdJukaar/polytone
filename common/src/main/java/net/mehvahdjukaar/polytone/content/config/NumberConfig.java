package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class NumberConfig extends PolyConfig<Float> implements OptionInstance.SliderableValueSet<Float> {

    public static final Codec<NumberConfig> CODEC = RecordCodecBuilder.<NumberConfig>create(instance ->
            commonFields(instance, Codec.FLOAT)
                    .and(instance.group(
                            Codec.FLOAT.optionalFieldOf("min", 0f).forGetter(c -> c.min),
                            Codec.FLOAT.optionalFieldOf("max", 1f).forGetter(c -> c.max),
                            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("step", 0.1f).forGetter(c -> c.step)
                    )).apply(instance, NumberConfig::new)).validate(PolyConfig::validatePresets);

    private final float step;
    private final float min;
    private final float max;

    protected NumberConfig(Optional<String> valueTranslation, Map<String, Float> presets, int order, float defaultValue, float min, float max, float step) {
        super(valueTranslation, presets, order, defaultValue);
        this.step = step;
        this.min = min;
        this.max = max;
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
