package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A single Polytone post-shader entry parsed from {@code polytone/post_shaders/*.json}.
 *
 * <p>Schema (matches 1.21.11):
 * <pre>{@code
 * {
 *   "post_chain": "namespace:effect_name",        // refers to assets/namespace/shaders/post/effect_name.json
 *   "activation_condition": "<MVEL expr, >0 enables>",  // optional, defaults to always-on
 *   "expression_uniforms": {                      // optional, name -> MVEL expression (float)
 *       "MyUniform": "<MVEL expr>"                // applied to every pass via PostChain.setUniform
 *   },
 *   "priority": 0.0                               // 1.21.1-only: controls layering vs other Polytone effects
 * }
 * }</pre>
 */
public final class PostChainEffect {

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
            ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
            Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)
                    .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.expressionUniforms),
            Codec.FLOAT.optionalFieldOf("priority", 0f).forGetter(p -> p.priority)
    ).apply(i, PostChainEffect::new));

    private final ResourceLocation postChain;
    private final ISimpleExp turnOnCondition;
    private final Map<String, ISimpleExp> expressionUniforms;
    private final float priority;

    public PostChainEffect(ResourceLocation postChain,
                           ISimpleExp turnOnCondition,
                           Map<String, ISimpleExp> expressionUniforms,
                           float priority) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.expressionUniforms = expressionUniforms;
        this.priority = priority;
    }

    public ResourceLocation postChain() {
        return postChain;
    }

    public float priority() {
        return priority;
    }

    /** Resource path of the vanilla post chain JSON file, e.g. {@code namespace:shaders/post/effect_name.json}. */
    public ResourceLocation chainResource() {
        return postChain.withPath(p -> "post_effect/" + p + ".json");
    }

    public boolean shouldBeOn() {
        return turnOnCondition.evaluate() > 0;
    }

    /** Re-evaluate every {@code expression_uniforms} entry and push the float value into the chain. */
    public void applyExpressionUniforms(PostChain chain) {
        if (expressionUniforms.isEmpty()) return;
        for (var e : expressionUniforms.entrySet()) {
            chain.setUniform(e.getKey(), (float) e.getValue().evaluate());
        }
    }
}
