package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A single Polytone post-shader entry parsed from {@code polytone/post_shaders/*.json}.
 *
 * <p>Schema (matches 1.21.11):
 * <pre>{@code
 * {
 *   "post_chain": "namespace:effect_name",        // refers to assets/namespace/post_effect/effect_name.json
 *   "activation_condition": "<MVEL expr, >0 enables>",  // optional, defaults to always-on
 *   "expression_uniforms": {                      // optional, name -> MVEL expression (float)
 *       "MyUniform": "<MVEL expr>"                // applied to every pass
 *   },
 *   "use_depth_buffer": false,                    // optional, exposes the level depth as the "InDepth" sampler
 *   "samplers": {                                 // optional, sampler name -> texture resource location
 *       "MySampler": "namespace:textures/effect/noise.png"  // bound as "sampler2D MySampler" on every pass
 *   },
 *   "priority": 0.0                               // 1.21.1-only: controls layering vs other Polytone effects
 * }
 * }</pre>
 *
 * <p><b>Built-in uniforms.</b> Every pass shader may declare any of these and Polytone fills them in
 * each frame (the 1.21.1 equivalent of the 1.21.11 {@code PolyGlobals} UBO block):
 * <ul>
 *     <li>{@code uniform mat4 PolyProjMat} - the level projection matrix</li>
 *     <li>{@code uniform mat4 PolyModelViewMat} - the camera/view (model-view) matrix</li>
 *     <li>{@code uniform float PolySunAngle} - sun angle in radians (0 = noon, like 1.21.11)</li>
 *     <li>{@code uniform float PolyDayTime} - world day time in ticks (0..24000)</li>
 *     <li>{@code uniform float PolyDeltaTime} - frame delta time in ticks (real render delta)</li>
 *     <li>{@code uniform ivec3 PolyPlayerBlockPos} / {@code uniform vec3 PolyPlayerOffset} - lerped player (feet)
 *         position, split for float precision at large coords: {@code exact = vec3(PolyPlayerBlockPos) - PolyPlayerOffset}</li>
 *     <li>{@code uniform sampler2D InDepth} - level depth texture, only bound when {@code use_depth_buffer} is set</li>
 *     <li>{@code uniform sampler2D InShadow} + {@code uniform mat4 PolyShadowMat} - directional shadow depth map
 *         and its light view-projection (camera-relative space), only bound when {@code use_shadow_map} is set.
 *         Transform a reconstructed camera-relative world position by {@code PolyShadowMat}, do the perspective
 *         divide, map to {@code [0,1]}, and compare against {@code InShadow} to test occlusion.</li>
 * </ul>
 * Shaders that don't declare a given uniform/sampler are unaffected ({@code safeGetUniform} no-ops).
 */
public final class PostChainEffect {

    public static final String PROJ_MAT = "PolyProjMat";
    public static final String MODEL_VIEW_MAT = "PolyModelViewMat";
    public static final String SUN_ANGLE = "PolySunAngle";
    public static final String DAY_TIME = "PolyDayTime";
    public static final String DELTA_TIME = "PolyDeltaTime";
    public static final String PLAYER_BLOCK_POS = "PolyPlayerBlockPos";
    public static final String PLAYER_OFFSET = "PolyPlayerOffset";
    public static final String DEPTH_SAMPLER = "InDepth";
    public static final String SHADOW_MAT = "PolyShadowMat";
    public static final String SHADOW_LIGHT_DIR = "PolyShadowLightDir";
    public static final String SHADOW_CAM_FRACT = "PolyShadowCamFract";
    public static final String SHADOW_SAMPLER = "InShadow";

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
            ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
            Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)
                    .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.expressionUniforms),
            Codec.BOOL.optionalFieldOf("use_depth_buffer", false).forGetter(p -> p.useDepthBuffer),
            Codec.BOOL.optionalFieldOf("use_shadow_map", false).forGetter(p -> p.useShadowMap),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("samplers", Map.of()).forGetter(p -> p.samplers),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("target_samplers", Map.of()).forGetter(p -> p.targetSamplers),
            Codec.FLOAT.optionalFieldOf("priority", 0f).forGetter(p -> p.priority)
    ).apply(i, PostChainEffect::new));

    private final ResourceLocation postChain;
    private final ISimpleExp turnOnCondition;
    private final Map<String, ISimpleExp> expressionUniforms;
    private final boolean useDepthBuffer;
    private final boolean useShadowMap;
    private final Map<String, ResourceLocation> samplers;
    // Sampler name -> persistent PostTargetsManager target id; bound to that target's color texture.
    private final Map<String, ResourceLocation> targetSamplers;
    private final float priority;

    public PostChainEffect(ResourceLocation postChain,
                           ISimpleExp turnOnCondition,
                           Map<String, ISimpleExp> expressionUniforms,
                           boolean useDepthBuffer,
                           boolean useShadowMap,
                           Map<String, ResourceLocation> samplers,
                           Map<String, ResourceLocation> targetSamplers,
                           float priority) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.expressionUniforms = expressionUniforms;
        this.useDepthBuffer = useDepthBuffer;
        this.useShadowMap = useShadowMap;
        this.samplers = samplers;
        this.targetSamplers = targetSamplers;
        this.priority = priority;
    }

    public ResourceLocation postChain() {
        return postChain;
    }

    public float priority() {
        return priority;
    }

    public boolean useDepthBuffer() {
        return useDepthBuffer;
    }

    public boolean useShadowMap() {
        return useShadowMap;
    }

    /** Resource path of the post chain JSON file, e.g. {@code namespace:post_effect/effect_name.json} (1.21.11 location, shared with packs made for it). */
    public ResourceLocation chainResource() {
        return postChain.withPath(p -> "post_effect/" + p + ".json");
    }

    public boolean shouldBeOn() {
        return turnOnCondition.evaluate() > 0;
    }

    /**
     * Push all per-frame uniforms for a single pass effect. Called from {@code PostPassMixin}
     * immediately before {@code EffectInstance.apply()} so sampler/uniform state matches what
     * {@code PostPass.process} just configured (notably {@code DiffuseSampler}).
     */
    public void applyUniformsToEffect(EffectInstance effect, PostShadersManager.ActivePostPassFrame frame) {
        // Vanilla blit passes share the chain but don't declare Polytone uniforms; skip them.
        if (effect.getUniform(PROJ_MAT) == null) return;
        effect.safeGetUniform(PROJ_MAT).set(frame.projMat());
        effect.safeGetUniform(MODEL_VIEW_MAT).set(frame.modelViewMat());
        effect.safeGetUniform(SUN_ANGLE).set(frame.sunAngle());
        effect.safeGetUniform(DAY_TIME).set(frame.dayTime());
        effect.safeGetUniform(DELTA_TIME).set(frame.deltaTime());
        var bp = frame.playerBlockPos();
        effect.safeGetUniform(PLAYER_BLOCK_POS).set(bp.getX(), bp.getY(), bp.getZ());
        var off = frame.playerOffset();
        effect.safeGetUniform(PLAYER_OFFSET).set((float) off.x, (float) off.y, (float) off.z);
        for (var e : expressionUniforms.entrySet()) {
            effect.safeGetUniform(e.getKey()).set((float) e.getValue().evaluate());
        }
        if (useDepthBuffer && frame.depthTexture() != null) {
            effect.setSampler(DEPTH_SAMPLER, frame.depthTexture());
        }
        if (useShadowMap) {
            // Light view-projection + light direction + the shadow depth map rendered this frame (see ShadowMapRenderer).
            ShadowMapRenderer shadows = Polytone.SHADOWS.renderer();
            effect.safeGetUniform(SHADOW_MAT).set(shadows.getShadowMatrix());
            var dir = shadows.getLightDir();
            effect.safeGetUniform(SHADOW_LIGHT_DIR).set(dir.x, dir.y, dir.z);
            // Camera fract, letting the pass snap camera-relative positions to a world-aligned block grid.
            var fract = shadows.getCamFract();
            effect.safeGetUniform(SHADOW_CAM_FRACT).set(fract.x, fract.y, fract.z);
            effect.setSampler(SHADOW_SAMPLER, shadows::getShadowTextureId);
        }
        for (var e : samplers.entrySet()) {
            ResourceLocation texture = e.getValue();
            effect.setSampler(e.getKey(),
                    () -> Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
        }
        // Persistent post targets bound as samplers (read side of the 1.21.1 post-targets port).
        for (var e : targetSamplers.entrySet()) {
            ResourceLocation targetId = e.getValue();
            effect.setSampler(e.getKey(), () -> {
                var t = Polytone.POST_TARGETS.getTarget(targetId);
                return t == null ? 0 : t.getColorTextureId();
            });
        }
    }
}
