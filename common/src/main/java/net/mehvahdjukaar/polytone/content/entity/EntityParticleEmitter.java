package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.TokenBucketTracker;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.EmfCompat;
import net.mehvahdjukaar.polytone.compat.EtfCompat;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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

    public static final SchemaCodec<EntityParticleEmitter> CODEC = SchemaRecord.create(EntityParticleEmitter.class, i -> i.group(
                    i.optional("bone", BONE_CODEC, List.of(), EntityParticleEmitter::bone),
                    i.optional("target_texture", SchemaCodecs.predicate(Identifier.CODEC), EntityParticleEmitter::textureId),
                    i.optional("target_etf_variant", SchemaCodecs.predicate(Codec.INT), EntityParticleEmitter::etfVariant),
                    i.optional("target_emf_variant", SchemaCodecs.predicate(Codec.INT), EntityParticleEmitter::emfVariant),
                    i.field("particle", CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE), EntityParticleEmitter::particleType),
                    i.optional("max_distance", Codec.INT, 32, EntityParticleEmitter::maxDistance),
                    i.optional("chance", IEntityExp.CODEC, IEntityExp.ONE, EntityParticleEmitter::chance),
                    i.optional("count", IEntityExp.CODEC, IEntityExp.ONE, EntityParticleEmitter::count),
                    i.optional("x", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::x),
                    i.optional("y", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::y),
                    i.optional("z", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::z),
                    i.optional("dx", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::dx),
                    i.optional("dy", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::dy),
                    i.optional("dz", IEntityExp.CODEC, IEntityExp.ZERO, EntityParticleEmitter::dz),
                    i.optional("r", IEntityExp.CODEC, EntityParticleEmitter::r),
                    i.optional("g", IEntityExp.CODEC, EntityParticleEmitter::g),
                    i.optional("b", IEntityExp.CODEC, EntityParticleEmitter::b),
                    i.optional("a", IEntityExp.CODEC, EntityParticleEmitter::a),
                    i.optional("roll", IEntityExp.CODEC, EntityParticleEmitter::roll),
                    i.optional("size", IEntityExp.CODEC, EntityParticleEmitter::size),
                    i.optional("custom", IEntityExp.CODEC, EntityParticleEmitter::custom)
            ).apply(i, EntityParticleEmitter::new));

    public void tick(Entity entity, Matrix4fc transform) {
        if (particleType.isEmpty()) return;
        Level level = entity.level();
        float throttle = Polytone.CONFIGS.particlesThrottle.get();
        if (throttle < 1 && level.getRandom().nextFloat() > throttle) return;

        double spawnChance = chance.evaluate(entity);
        if (level.getRandom().nextFloat() < spawnChance) {
            for (int i = 0; i < count.evaluate(entity); i++) {
                ParticleOptions po = getParticleOptions(entity);
                if (po == null) return;
                if(!TokenBucketTracker.canEmitParticle(this))return;
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
                // Apply the full matrix to position
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
    public PoseStack getSpawnPosWithoutModel(Entity entity, Vec3 cameraPos){
        //check distance
        double distSq = cameraPos.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
        if (distSq > maxDistance * maxDistance) {
            return null;
        }

        //find bone
        if (bone.isEmpty()) {
            //no bone is given, spawn at entity position
            return new PoseStack();
        }
        return null;
    }


    @Nullable
    public <S extends EntityRenderState> PoseStack getModelSpawnPose(
            Model<? super S> model, S state, EntityRenderer<?, S> renderer, Vec3 cameraPos) {

        if (textureId.isPresent() && renderer instanceof LivingEntityRenderer lr &&
                !textureId.get().test(lr.getTextureLocation((LivingEntityRenderState) state))) {
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
        double distSq = cameraPos.distanceToSqr(state.x, state.y, state.z);
        if (distSq > maxDistance * maxDistance) {
            return null;
        }

        //find bone
        if (bone.isEmpty()) {
            //no bone is given, spawn at entity position
            return new PoseStack();
        }


        ModelPart part = model.root();
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
