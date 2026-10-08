package net.mehvahdjukaar.polytone.content.shaders.post;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.ExpressionUniforms;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class PostChainEffect {

    public static final String DEPTH_SAMPLER = "InDepth";

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
            ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.activationCondition),
            ExpressionUniforms.CODEC.optionalFieldOf("expression_uniforms", ExpressionUniforms.EMPTY).forGetter(p -> p.expressionUniforms),
            Codec.BOOL.optionalFieldOf("use_depth_buffer", false).forGetter(p -> p.useDepthBuffer),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("samplers", Map.of()).forGetter(p -> p.samplers),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("target_samplers", Map.of()).forGetter(p -> p.targetSamplers),
            Codec.FLOAT.optionalFieldOf("priority", 0f).forGetter(p -> p.priority)
    ).apply(i, PostChainEffect::new));

    private final ResourceLocation postChain;
    private final ISimpleExp activationCondition;
    private final ExpressionUniforms expressionUniforms;
    private final boolean useDepthBuffer;
    private final Map<String, ResourceLocation> samplers;
    private final Map<String, ResourceLocation> targetSamplers;
    private final float priority;

    public PostChainEffect(ResourceLocation postChain,
                           ISimpleExp activationCondition,
                           ExpressionUniforms expressionUniforms,
                           boolean useDepthBuffer,
                           Map<String, ResourceLocation> samplers,
                           Map<String, ResourceLocation> targetSamplers,
                           float priority) {
        this.postChain = postChain;
        this.activationCondition = activationCondition;
        this.expressionUniforms = expressionUniforms;
        this.useDepthBuffer = useDepthBuffer;
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

    public ResourceLocation chainResource() {
        return postChain.withPath(p -> "post_effect/" + p + ".json");
    }

    public boolean shouldBeOn() {
        return activationCondition.evaluate() > 0;
    }

    public void bindTo(IShader inputs, int depthTextureId) {
        for (IShaderModifier modifier : Polytone.SHADER_MODIFIERS) {
            if (modifier.isUsedBy(inputs)){
                modifier.bindTo(inputs);
            }
        }
        expressionUniforms.evaluate();
        expressionUniforms.bindTo(inputs);
        if (useDepthBuffer) inputs.setSampler(DEPTH_SAMPLER, depthTextureId);

        var textures = Minecraft.getInstance().getTextureManager();
        for (var e : samplers.entrySet()) {
            inputs.setSampler(e.getKey(), textures.getTexture(e.getValue()).getId());
        }
        for (var e : targetSamplers.entrySet()) {
            var t = Polytone.POST_TARGETS.getTarget(e.getValue());
            inputs.setSampler(e.getKey(), t == null ? 0 : t.getColorTextureId());
        }
    }
}
