package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.biome.BiomeEffectModifier;
import net.mehvahdjukaar.polytone.content.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.content.particle.BlockParticleEmitter;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleTickable;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record EntityParticleEmitter(
        String bone,
        Optional<Holder<ParticleType<?>>> particleType,
        IEntityExp chance,
        IEntityExp count,
        IEntityExp x,
        IEntityExp y,
        IEntityExp z,
        IEntityExp dx,
        IEntityExp dy,
        IEntityExp dz,
        Optional<IEntityExp> r,
        Optional<IEntityExp> g,
        Optional<IEntityExp> b,
        Optional<IEntityExp> a,
        Optional<IEntityExp> roll,
        Optional<IEntityExp> size,
        Optional<IEntityExp> custom
) {

    public static final Codec<EntityParticleEmitter> CODEC = RecordCodecBuilder.create(
        i -> BiggerCodecs.group(i,
                Codec.STRING.fieldOf("bone").forGetter(EntityParticleEmitter::bone),
                CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle")
                        .forGetter(EntityParticleEmitter::particleType),
                IEntityExp.CODEC.optionalFieldOf("chance", IEntityExp.ONE).forGetter(EntityParticleEmitter::chance),
                IEntityExp.CODEC.optionalFieldOf("count", IEntityExp.ONE).forGetter(EntityParticleEmitter::count),
                IEntityExp.CODEC.optionalFieldOf("x", IEntityExp.ZERO).forGetter(EntityParticleEmitter::x),
                IEntityExp.CODEC.optionalFieldOf("y", IEntityExp.ZERO).forGetter(EntityParticleEmitter::y),
                IEntityExp.CODEC.optionalFieldOf("z", IEntityExp.ZERO).forGetter(EntityParticleEmitter::z),
                IEntityExp.CODEC.optionalFieldOf("dx", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dx),
                IEntityExp.CODEC.optionalFieldOf("dy", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dy),
                IEntityExp.CODEC.optionalFieldOf("dz", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dz),
                IEntityExp.CODEC.optionalFieldOf("r").forGetter(EntityParticleEmitter::r),
                IEntityExp.CODEC.optionalFieldOf("g").forGetter(EntityParticleEmitter::g),
                IEntityExp.CODEC.optionalFieldOf("b").forGetter(EntityParticleEmitter::b),
                IEntityExp.CODEC.optionalFieldOf("a").forGetter(EntityParticleEmitter::a),
                IEntityExp.CODEC.optionalFieldOf("roll").forGetter(EntityParticleEmitter::roll),
                IEntityExp.CODEC.optionalFieldOf("size").forGetter(EntityParticleEmitter::size),
                IEntityExp.CODEC.optionalFieldOf("custom").forGetter(EntityParticleEmitter::custom)
        ).apply(i, EntityParticleEmitter::new));

    public void tick(Entity entity, Vec3 origin) {
        if (particleType.isEmpty()) return;
        double spawnChance = chance.evaluate(entity);
        Level level = entity.level();
        if (level.random.nextFloat() < spawnChance) {
            for (int i = 0; i < count.evaluate(entity); i++) {
                ParticleOptions po = getParticleOptions(entity);
                if (po == null) return;
                level.addParticle(po,
                        origin.x + x.evaluate(entity),
                        origin.y + y.evaluate(entity),
                        origin.z + z.evaluate(entity),
                        dx.evaluate(entity),
                        dy.evaluate(entity),
                        dz.evaluate(entity)
                );
            }
        }
    }


    private @Nullable ParticleOptions getParticleOptions(Entity entity) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().identifier())) {
            Map<String, Float> map = new HashMap<>();
            r.ifPresent(exp -> map.put("red", (float) exp.evaluate(entity)));
            g.ifPresent(exp -> map.put("green", (float) exp.evaluate(entity)));
            b.ifPresent(exp -> map.put("blue", (float) exp.evaluate(entity)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.evaluate(entity)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.evaluate(entity)));
            size.ifPresent(exp -> map.put("size", (float) exp.evaluate(entity)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.evaluate(entity)));
            return new ExtraDataParticleOptions(map, particleTypeValue);
        }

        if (particleTypeValue instanceof SimpleParticleType st) {
            po = st;
        } else {
            Polytone.LOGGER.error("Unsupported particle type: {}", particleTypeValue);
            return null;
        }
        return po;
    }

}
