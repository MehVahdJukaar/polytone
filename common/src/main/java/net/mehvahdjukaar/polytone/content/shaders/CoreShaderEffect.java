package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class CoreShaderEffect {

    public static final Codec<CoreShaderEffect> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("shader").forGetter(p -> p.shader),
                    ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
                    ExpressionUniformBuffers.MAP_CODEC
                            .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.buffers.expressions())
            ).apply(i, CoreShaderEffect::new));

    private final Identifier shader;
    private final ISimpleExp turnOnCondition;
    private final ExpressionUniformBuffers buffers;

    private boolean cachedOn = false;

    public CoreShaderEffect(Identifier shader, ISimpleExp turnOnCondition, Map<String, ISimpleExp> expressionUniforms) {
        this.shader = shader;
        this.turnOnCondition = turnOnCondition;
        this.buffers = new ExpressionUniformBuffers(expressionUniforms);
    }

    public Identifier shader() {
        return shader;
    }

    public ExpressionUniformBuffers buffers() {
        return buffers;
    }

    public void refreshEnabled() {
        cachedOn = turnOnCondition.evaluate() > 0;
    }

    public boolean isOn() {
        return cachedOn;
    }
}
