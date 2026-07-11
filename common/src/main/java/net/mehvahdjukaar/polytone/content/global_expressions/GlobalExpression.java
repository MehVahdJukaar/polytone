package net.mehvahdjukaar.polytone.content.global_expressions;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.util.ExtraCodecs;

public record GlobalExpression(ISimpleExp exp, int updateInterval, double defaultValue) {

    public static final SchemaCodec<GlobalExpression> CODEC = SchemaRecord.create(GlobalExpression.class, i ->
            i.group(
                    i.field("expression", ISimpleExp.CODEC, GlobalExpression::exp),
                    i.optional("update_interval", ExtraCodecs.POSITIVE_INT, 1, GlobalExpression::updateInterval),
                    i.field("default_value", Codec.DOUBLE, GlobalExpression::defaultValue)
            ).apply(i, GlobalExpression::new)
    );
}
