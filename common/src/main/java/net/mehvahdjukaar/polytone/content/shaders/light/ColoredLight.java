package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;

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

    public static PointLightProvider.ForBlock forBlocks(ColoredLight<IBlockExp> light) {
        return (state, pos, level, r) -> light.resolve(exp -> exp.evaluate(level, pos, state), r);
    }

    public static PointLightProvider<Entity> forEntities(ColoredLight<IEntityExp> light) {
        return (entity, level, r) -> light.resolve(exp -> exp.evaluate(entity), r);
    }

    public static PointLightProvider.ForItem forItems(ColoredLight<IEntityExp> light) {
        return (stack, holder, level, r) -> light.resolve(exp -> exp.evaluate(holder), r);
    }

    public static PointLightProvider<Particle> forParticles(ColoredLight<IParticleExp> light) {
        return (particle, level, r) -> light.resolve(exp -> exp.evaluate(particle, level), r);
    }

    public ResolvedPointLight resolve(Eval<E> eval, float defaultRadius) {
        return new ResolvedPointLight(
                (int) eval.apply(color) & 0xFFFFFF,
                radius.map(e -> (float) eval.apply(e)).orElse(defaultRadius),
                brightness.map(value -> (float) eval.apply(value)).orElse(1F));
    }
}
