package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.function.IntSupplier;

/**
 * A single Polytone post-shader entry parsed from {@code polytone/post_shaders/*.json}.
 *
 * <p>Schema (matches 1.21.11):
 * <pre>{@code
 * {
 *   "post_chain": "namespace:effect_name",        // refers to assets/namespace/shaders/post/effect_name.json
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
 *     <li>{@code uniform mat4 PolyProjMat} — the level projection matrix</li>
 *     <li>{@code uniform mat4 PolyModelViewMat} — the camera/view (model-view) matrix</li>
 *     <li>{@code uniform float PolySunAngle} — sun angle in radians (0 = noon, like 1.21.11)</li>
 *     <li>{@code uniform float PolyDayTime} — world day time in ticks (0..24000)</li>
 *     <li>{@code uniform sampler2D InDepth} — level depth texture, only bound when {@code use_depth_buffer} is set</li>
 * </ul>
 * Shaders that don't declare a given uniform/sampler are unaffected ({@code safeGetUniform} no-ops).
 */
public final class PostChainEffect {

    public static final String PROJ_MAT = "PolyProjMat";
    public static final String MODEL_VIEW_MAT = "PolyModelViewMat";
    public static final String SUN_ANGLE = "PolySunAngle";
    public static final String DAY_TIME = "PolyDayTime";
    public static final String DEPTH_SAMPLER = "InDepth";

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
            ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
            Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)
                    .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.expressionUniforms),
            Codec.BOOL.optionalFieldOf("use_depth_buffer", false).forGetter(p -> p.useDepthBuffer),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("samplers", Map.of()).forGetter(p -> p.samplers),
            Codec.FLOAT.optionalFieldOf("priority", 0f).forGetter(p -> p.priority)
    ).apply(i, PostChainEffect::new));

    private final ResourceLocation postChain;
    private final ISimpleExp turnOnCondition;
    private final Map<String, ISimpleExp> expressionUniforms;
    private final boolean useDepthBuffer;
    private final Map<String, ResourceLocation> samplers;
    private final float priority;

    public PostChainEffect(ResourceLocation postChain,
                           ISimpleExp turnOnCondition,
                           Map<String, ISimpleExp> expressionUniforms,
                           boolean useDepthBuffer,
                           Map<String, ResourceLocation> samplers,
                           float priority) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.expressionUniforms = expressionUniforms;
        this.useDepthBuffer = useDepthBuffer;
        this.samplers = samplers;
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

    /** Resource path of the vanilla post chain JSON file, e.g. {@code namespace:shaders/post/effect_name.json}. */
    public ResourceLocation chainResource() {
        return postChain.withPath(p -> "post_effect/" + p + ".json");
    }

    public boolean shouldBeOn() {
        return turnOnCondition.evaluate() > 0;
    }

    /**
     * Push all per-frame uniforms (built-in globals + re-evaluated {@code expression_uniforms}) and,
     * when enabled, the depth sampler into every pass of the chain. Must run before {@code chain.process}
     * so the values are live when each pass calls {@code effect.apply()}.
     *
     * @param depthTexture supplier of the level depth texture id; only bound when {@link #useDepthBuffer} is set
     */
    public void applyUniforms(PostChain chain, Matrix4f projMat, Matrix4f modelViewMat,
                              float sunAngle, float dayTime, @Nullable IntSupplier depthTexture) {
        for (PostPass pass : chain.passes) {
            EffectInstance effect = pass.getEffect();
            effect.safeGetUniform(PROJ_MAT).set(projMat);
            effect.safeGetUniform(MODEL_VIEW_MAT).set(modelViewMat);
            effect.safeGetUniform(SUN_ANGLE).set(sunAngle);
            effect.safeGetUniform(DAY_TIME).set(dayTime);
            for (var e : expressionUniforms.entrySet()) {
                effect.safeGetUniform(e.getKey()).set((float) e.getValue().evaluate());
            }
            if (useDepthBuffer && depthTexture != null) {
                effect.setSampler(DEPTH_SAMPLER, depthTexture);
            }
            for (var e : samplers.entrySet()) {
                ResourceLocation texture = e.getValue();
                effect.setSampler(e.getKey(),
                        () -> Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
            }
        }
    }
}
