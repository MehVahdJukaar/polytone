package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;

import java.util.LinkedHashMap;
import java.util.Map;

public class BedrockComponentTypes {

    private static final Map<String, BedrockComponentType<?>> BY_ID = new LinkedHashMap<>();

    // emitter
    public static final BedrockComponentType<EmitterComponents.Initialization> EMITTER_INITIALIZATION =
            register("emitter_initialization", EmitterComponents.Initialization.CODEC);
    public static final BedrockComponentType<EmitterComponents.LocalSpace> EMITTER_LOCAL_SPACE =
            register("emitter_local_space", EmitterComponents.LocalSpace.CODEC);
    public static final BedrockComponentType<EmitterComponents.RateInstant> EMITTER_RATE_INSTANT =
            register("emitter_rate_instant", EmitterComponents.RateInstant.CODEC);
    public static final BedrockComponentType<EmitterComponents.RateSteady> EMITTER_RATE_STEADY =
            register("emitter_rate_steady", EmitterComponents.RateSteady.CODEC);
    public static final BedrockComponentType<EmitterComponents.RateManual> EMITTER_RATE_MANUAL =
            register("emitter_rate_manual", EmitterComponents.RateManual.CODEC);
    public static final BedrockComponentType<EmitterComponents.LifetimeOnce> EMITTER_LIFETIME_ONCE =
            register("emitter_lifetime_once", EmitterComponents.LifetimeOnce.CODEC);
    public static final BedrockComponentType<EmitterComponents.LifetimeLooping> EMITTER_LIFETIME_LOOPING =
            register("emitter_lifetime_looping", EmitterComponents.LifetimeLooping.CODEC);
    public static final BedrockComponentType<EmitterComponents.LifetimeExpression> EMITTER_LIFETIME_EXPRESSION =
            register("emitter_lifetime_expression", EmitterComponents.LifetimeExpression.CODEC);
    public static final BedrockComponentType<EmitterComponents.LifetimeEvents> EMITTER_LIFETIME_EVENTS =
            register("emitter_lifetime_events", EmitterComponents.LifetimeEvents.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapePoint> EMITTER_SHAPE_POINT =
            register("emitter_shape_point", EmitterComponents.ShapePoint.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapeSphere> EMITTER_SHAPE_SPHERE =
            register("emitter_shape_sphere", EmitterComponents.ShapeSphere.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapeBox> EMITTER_SHAPE_BOX =
            register("emitter_shape_box", EmitterComponents.ShapeBox.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapeDisc> EMITTER_SHAPE_DISC =
            register("emitter_shape_disc", EmitterComponents.ShapeDisc.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapeEntityAabb> EMITTER_SHAPE_ENTITY_AABB =
            register("emitter_shape_entity_aabb", EmitterComponents.ShapeEntityAabb.CODEC);
    public static final BedrockComponentType<EmitterComponents.ShapeCustom> EMITTER_SHAPE_CUSTOM =
            register("emitter_shape_custom", EmitterComponents.ShapeCustom.CODEC);

    // particle
    public static final BedrockComponentType<ParticleComponents.Initialization> PARTICLE_INITIALIZATION =
            register("particle_initialization", ParticleComponents.Initialization.CODEC);
    public static final BedrockComponentType<ParticleComponents.InitialSpeed> PARTICLE_INITIAL_SPEED =
            register("particle_initial_speed", ParticleComponents.InitialSpeed.CODEC);
    public static final BedrockComponentType<ParticleComponents.InitialSpin> PARTICLE_INITIAL_SPIN =
            register("particle_initial_spin", ParticleComponents.InitialSpin.CODEC);
    public static final BedrockComponentType<ParticleComponents.MotionDynamic> PARTICLE_MOTION_DYNAMIC =
            register("particle_motion_dynamic", ParticleComponents.MotionDynamic.CODEC);
    public static final BedrockComponentType<ParticleComponents.MotionParametric> PARTICLE_MOTION_PARAMETRIC =
            register("particle_motion_parametric", ParticleComponents.MotionParametric.CODEC);
    public static final BedrockComponentType<ParticleComponents.MotionCollision> PARTICLE_MOTION_COLLISION =
            register("particle_motion_collision", ParticleComponents.MotionCollision.CODEC);
    public static final BedrockComponentType<ParticleComponents.AppearanceBillboard> PARTICLE_APPEARANCE_BILLBOARD =
            register("particle_appearance_billboard", ParticleComponents.AppearanceBillboard.CODEC);
    public static final BedrockComponentType<ParticleComponents.AppearanceTinting> PARTICLE_APPEARANCE_TINTING =
            register("particle_appearance_tinting", ParticleComponents.AppearanceTinting.CODEC);
    public static final BedrockComponentType<ParticleComponents.AppearanceLighting> PARTICLE_APPEARANCE_LIGHTING =
            register("particle_appearance_lighting", ParticleComponents.AppearanceLighting.CODEC);
    public static final BedrockComponentType<ParticleComponents.LifetimeExpression> PARTICLE_LIFETIME_EXPRESSION =
            register("particle_lifetime_expression", ParticleComponents.LifetimeExpression.CODEC);
    public static final BedrockComponentType<ParticleComponents.LifetimeEvents> PARTICLE_LIFETIME_EVENTS =
            register("particle_lifetime_events", ParticleComponents.LifetimeEvents.CODEC);
    public static final BedrockComponentType<ParticleComponents.KillPlane> PARTICLE_KILL_PLANE =
            register("particle_kill_plane", ParticleComponents.KillPlane.CODEC);
    public static final BedrockComponentType<ParticleComponents.ExpireInBlocks> PARTICLE_EXPIRE_IF_IN_BLOCKS =
            register("particle_expire_if_in_blocks", ParticleComponents.ExpireInBlocks.CODEC);
    public static final BedrockComponentType<ParticleComponents.ExpireNotInBlocks> PARTICLE_EXPIRE_IF_NOT_IN_BLOCKS =
            register("particle_expire_if_not_in_blocks", ParticleComponents.ExpireNotInBlocks.CODEC);

    private static <T> BedrockComponentType<T> register(String id, Codec<T> codec) {
        BedrockComponentType<T> type = new BedrockComponentType<>(id, codec);
        BY_ID.put(id, type);
        return type;
    }

    public static boolean isKnown(String id) {
        return BY_ID.containsKey(id);
    }
}
