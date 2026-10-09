package net.mehvahdjukaar.polytone.content.shaders.light;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.DoubleFunction;

//interface madness
public sealed interface ColoredLight<E> permits ColoredLight.BlockLight, ColoredLight.EntityLight, ColoredLight.ParticleLight {

    E color();

    Optional<E> radius();

    Optional<E> brightness();

    private static <E, L extends ColoredLight<E>> Codec<L> codec(Codec<E> expression, DoubleFunction<E> constant,
                                                               Function3<E, Optional<E>, Optional<E>, L> factory) {
        Codec<E> color = Codec.withAlternative(ColorUtils.CODEC.xmap(constant::apply, e -> 0), expression);
        Codec<L> full = RecordCodecBuilder.create(i -> i.group(
                color.fieldOf("color").forGetter(ColoredLight::color),
                expression.optionalFieldOf("radius").forGetter(ColoredLight::radius),
                expression.optionalFieldOf("brightness").forGetter(ColoredLight::brightness)
        ).apply(i, factory));
        return Codec.withAlternative(full, color.xmap(c -> factory.apply(c, Optional.empty(), Optional.empty()), ColoredLight::color));
    }

    record BlockLight(IBlockExp color, Optional<IBlockExp> radius, Optional<IBlockExp> brightness)
            implements ColoredLight<IBlockExp>, PointLightProvider.ForBlock {

        public static final Codec<BlockLight> CODEC = codec(IBlockExp.CODEC, IBlockExp::constant, BlockLight::new);

        @Override
        public ResolvedPointLight resolve(BlockState state, Vec3 pos, ClientLevel level) {
            return new ResolvedPointLight((int) color.evaluate(level, pos, state) & 0xFFFFFF,
                    radius.map(r -> (float) r.evaluate(level, pos, state)).orElse(state.getLightEmission() > 0 ? state.getLightEmission() : 8F),
                    brightness.map(iBlockExp -> (float) iBlockExp.evaluate(level, pos, state)).orElse(1F));
        }
    }

    //items use these too, packs only see the holder
    record EntityLight(IEntityExp color, Optional<IEntityExp> radius, Optional<IEntityExp> brightness)
            implements ColoredLight<IEntityExp>, PointLightProvider<Entity>, PointLightProvider.ForItem {

        public static final Codec<EntityLight> CODEC = codec(IEntityExp.CODEC, c -> e -> c, EntityLight::new);

        @Override
        public ResolvedPointLight resolve(Entity entity, ClientLevel level) {
            return new ResolvedPointLight((int) color.evaluate(entity) & 0xFFFFFF,
                    radius.map(iEntityExp -> (float) iEntityExp.evaluate(entity)).orElse(8F),
                    brightness.map(entityExp -> (float) entityExp.evaluate(entity)).orElse(1F));
        }

        @Override
        public ResolvedPointLight resolve(ItemStack stack, Entity holder, ClientLevel level) {
            return resolve(holder, level);
        }
    }

    record ParticleLight(IParticleExp color, Optional<IParticleExp> radius, Optional<IParticleExp> brightness)
            implements ColoredLight<IParticleExp>, PointLightProvider<Particle> {

        public static final Codec<ParticleLight> CODEC = codec(IParticleExp.CODEC, c -> (p, l) -> c, ParticleLight::new);

        @Override
        public ResolvedPointLight resolve(Particle particle, ClientLevel level) {
            return new ResolvedPointLight((int) color.evaluate(particle, level) & 0xFFFFFF,
                    radius.map(iParticleExp -> (float) iParticleExp.evaluate(particle, level)).orElse(8F),
                    brightness.map(particleExp -> (float) particleExp.evaluate(particle, level)).orElse(1F));
        }
    }
}
