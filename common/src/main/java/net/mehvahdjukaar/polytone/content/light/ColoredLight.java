package net.mehvahdjukaar.polytone.content.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ColorUtils;

import java.util.Optional;
import java.util.function.DoubleFunction;

public record ColoredLight<E>(E color, Optional<E> radius, Optional<E> brightness) {

    @FunctionalInterface
    public interface Eval<E> {
        double apply(E expression);
    }

    public static <E> Codec<ColoredLight<E>> codec(Codec<E> expression, DoubleFunction<E> constant) {
        Codec<E> color = Codec.withAlternative(ColorUtils.CODEC.xmap(constant::apply, e -> 0), expression);
        Codec<ColoredLight<E>> full = RecordCodecBuilder.create(i -> i.group(
                color.fieldOf("color").forGetter(ColoredLight::color),
                expression.optionalFieldOf("radius").forGetter(ColoredLight::radius),
                expression.optionalFieldOf("brightness").forGetter(ColoredLight::brightness)
        ).apply(i, ColoredLight::new));
        return Codec.withAlternative(full, color.xmap(ColoredLight::of, ColoredLight::color));
    }

    public static <E> ColoredLight<E> of(E color) {
        return new ColoredLight<>(color, Optional.empty(), Optional.empty());
    }

    public LightProperties resolve(Eval<E> eval, float defaultRadius) {
        return new LightProperties(
                (int) eval.apply(color) & 0xFFFFFF,
                radius.isPresent() ? (float) eval.apply(radius.get()) : defaultRadius,
                brightness.isPresent() ? (float) eval.apply(brightness.get()) : 1);
    }
}
