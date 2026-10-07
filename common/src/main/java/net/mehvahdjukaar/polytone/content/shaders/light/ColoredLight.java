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
                color.fieldOf("color").forGetter(l -> l.color()),
                expression.optionalFieldOf("radius").forGetter(l -> l.radius()),
                expression.optionalFieldOf("brightness").forGetter(l -> l.brightness())
        ).apply(i, factory));
        return Codec.withAlternative(full, color.xmap(c -> factory.apply(c, Optional.empty(), Optional.empty()), l -> l.color()));
    }

    record BlockLight(IBlockExp color, Optional<IBlockExp> radius, Optional<IBlockExp> brightness)
            implements ColoredLight<IBlockExp>, PointLightProvider.ForBlock {

        public static final Codec<BlockLight> CODEC = codec(IBlockExp.CODEC, IBlockExp::constant, BlockLight::new);

        @Override
        public ResolvedPointLight resolve(BlockState state, Vec3 pos, ClientLevel level, float defaultRadius) {
            return new ResolvedPointLight((int) color.evaluate(level, pos, state) & 0xFFFFFF,
                    radius.isPresent() ? (float) radius.get().evaluate(level, pos, state) : defaultRadius,
                    brightness.isPresent() ? (float) brightness.get().evaluate(level, pos, state) : 1);
        }
    }

    //items use these too, packs only see the holder
    record EntityLight(IEntityExp color, Optional<IEntityExp> radius, Optional<IEntityExp> brightness)
            implements ColoredLight<IEntityExp>, PointLightProvider<Entity>, PointLightProvider.ForItem {

        public static final Codec<EntityLight> CODEC = codec(IEntityExp.CODEC, c -> e -> c, EntityLight::new);

        @Override
        public ResolvedPointLight resolve(Entity entity, ClientLevel level, float defaultRadius) {
            return new ResolvedPointLight((int) color.evaluate(entity) & 0xFFFFFF,
                    radius.isPresent() ? (float) radius.get().evaluate(entity) : defaultRadius,
                    brightness.isPresent() ? (float) brightness.get().evaluate(entity) : 1);
        }

        @Override
        public ResolvedPointLight resolve(ItemStack stack, Entity holder, ClientLevel level, float defaultRadius) {
            return resolve(holder, level, defaultRadius);
        }
    }

    record ParticleLight(IParticleExp color, Optional<IParticleExp> radius, Optional<IParticleExp> brightness)
            implements ColoredLight<IParticleExp>, PointLightProvider<Particle> {

        public static final Codec<ParticleLight> CODEC = codec(IParticleExp.CODEC, c -> (p, l) -> c, ParticleLight::new);

        @Override
        public ResolvedPointLight resolve(Particle particle, ClientLevel level, float defaultRadius) {
            return new ResolvedPointLight((int) color.evaluate(particle, level) & 0xFFFFFF,
                    radius.isPresent() ? (float) radius.get().evaluate(particle, level) : defaultRadius,
                    brightness.isPresent() ? (float) brightness.get().evaluate(particle, level) : 1);
        }
    }
}
