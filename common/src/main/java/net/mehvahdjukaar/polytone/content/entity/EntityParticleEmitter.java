package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.EmfCompat;
import net.mehvahdjukaar.polytone.compat.EtfCompat;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public record EntityParticleEmitter(
        List<String> bone,
        Optional<Predicate<Identifier>> textureId,
        Optional<Predicate<Integer>> etfVariant,
        Optional<Predicate<Integer>> emfVariant,
        Optional<Holder<ParticleType<?>>> particleType,
        int maxDistance,
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

    private static final Codec<List<String>> BONE_CODEC = Codec.STRING.xmap(
            s -> List.of(s.split("/.")),
            list -> String.join(".", list)
    );

    public static final Codec<EntityParticleEmitter> CODEC = RecordCodecBuilder.create(
            i -> BiggerCodecs.group(i,
                    BONE_CODEC.fieldOf("bone").forGetter(EntityParticleEmitter::bone),
                    CodecUtils.predicate(Identifier.CODEC).optionalFieldOf("target_texture").forGetter(EntityParticleEmitter::textureId),
                    CodecUtils.predicate(Codec.INT).optionalFieldOf("target_etf_variant").forGetter(EntityParticleEmitter::etfVariant),
                    CodecUtils.predicate(Codec.INT).optionalFieldOf("target_emf_variant").forGetter(EntityParticleEmitter::emfVariant),
                    CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle")
                            .forGetter(EntityParticleEmitter::particleType),
                    Codec.INT.optionalFieldOf("max_distance", 32).forGetter(EntityParticleEmitter::maxDistance),
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

    public void tick(Entity entity, Matrix4fc transform) {
        if (particleType.isEmpty()) return;
        double spawnChance = chance.evaluate(entity);
        Level level = entity.level();
        if (level.random.nextFloat() < spawnChance) {
            for (int i = 0; i < count.evaluate(entity); i++) {
                ParticleOptions po = getParticleOptions(entity);
                if (po == null) return;
                Vector3f origin = new Vector3f(
                        (float) x.evaluate(entity),
                        (float) y.evaluate(entity),
                        (float) z.evaluate(entity)
                );
                Vector3f speed = new Vector3f(
                        (float) dx.evaluate(entity),
                        (float) dy.evaluate(entity),
                        (float) dz.evaluate(entity)
                );
                // Apply the full transform to position
                origin.mulPosition(transform);

                // Apply rotation/scale only to velocity (no translation)
                transform.transformDirection(speed);
                level.addParticle(po,
                        origin.x,
                        origin.y,
                        origin.z,
                        speed.x,
                        speed.y,
                        speed.z
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


    @Nullable
    public <S extends LivingEntityRenderState> PoseStack getModelSpawnPose(LivingEntityRenderer<?, S, ?> renderer, S state) {
        //find bone
        if (bone.isEmpty()) {
            //no bone found, cancel spawning
            return null;
        }

        if (textureId.isPresent() && !textureId.get().test(renderer.getTextureLocation(state))) {
            return null;
        }

        //check emf
        if (emfVariant.isPresent() && CompatHandler.EMF && !emfVariant.get().test(
                        EmfCompat.getLastKnownTextureVariantIndex(state))) {
            return null;
        }
        //check etf
        if (etfVariant.isPresent() && CompatHandler.ETF && !etfVariant.get().test(
                EtfCompat.getLastKnownTextureVariantIndex(state))) {
            return null;
        }

        //check distance
        Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        double distSq = pos.distanceToSqr(state.x, state.y, state.z);
        if (distSq > maxDistance * maxDistance) {
            return null;
        }

        ModelPart part = renderer.getModel().root();
        List<ModelPart> parts = new ArrayList<>();
        parts.add(part);
        for (String b : bone) {
            part = part.children.get(b);
            if (part == null) {
                return null; //fail
            }
            parts.add(part);
        }
        PoseStack poseStack = new PoseStack();
        for (ModelPart p : parts) {
            p.translateAndRotate(poseStack);
        }

        return poseStack;
    }
}
