package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

// One post shader entry from polytone/post_shaders/*.json. The field reference and the list of
// built-in uniforms every pass may declare (PolyProjMat, PolySunAngle, InDepth, InShadow, ...) live
// in wiki/Shaders.md. Shaders that don't declare one are unaffected, safeGetUniform no-ops.
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

    // 1.21.11 location, shared with packs made for it
    public ResourceLocation chainResource() {
        return postChain.withPath(p -> "post_effect/" + p + ".json");
    }

    public boolean shouldBeOn() {
        return turnOnCondition.evaluate() > 0;
    }

    // called from PostPassMixin right before EffectInstance.apply(), so sampler/uniform state matches
    // what PostPass.process just configured (notably DiffuseSampler)
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
        for (var e : targetSamplers.entrySet()) {
            ResourceLocation targetId = e.getValue();
            effect.setSampler(e.getKey(), () -> {
                var t = Polytone.POST_TARGETS.getTarget(targetId);
                return t == null ? 0 : t.getColorTextureId();
            });
        }
    }
}
