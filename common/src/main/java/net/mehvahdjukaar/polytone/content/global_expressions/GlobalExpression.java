package net.mehvahdjukaar.polytone.content.global_expressions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.minecraft.util.ExtraCodecs;

public record GlobalExpression(ISimpleExp exp, int updateInterval, double defaultValue) {

    public static final Codec<GlobalExpression> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ISimpleExp.CODEC.fieldOf("expression").forGetter(GlobalExpression::exp),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("update_interval", 1).forGetter(GlobalExpression::updateInterval),
                    Codec.DOUBLE.fieldOf("default_value").forGetter(GlobalExpression::defaultValue)
            ).apply(instance, GlobalExpression::new)
    );
}
