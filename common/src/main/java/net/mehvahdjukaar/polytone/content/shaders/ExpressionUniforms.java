package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;

import java.util.Map;

public class ExpressionUniforms {

    public static final Codec<ExpressionUniforms> CODEC = SchemaCodecs.xmap(
            SchemaCodec.wrap(Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)),
            ExpressionUniforms::new, u -> u.expressions);

    public static final ExpressionUniforms EMPTY = new ExpressionUniforms(Map.of());

    private final Map<String, ISimpleExp> expressions;
    private final Object2FloatMap<String> values = new Object2FloatOpenHashMap<>();

    public ExpressionUniforms(Map<String, ISimpleExp> expressions) {
        this.expressions = expressions;
    }

    public void evaluate() {
        for (var e : expressions.entrySet()) {
            values.put(e.getKey(), (float) e.getValue().evaluate());
        }
    }

    //just copies what evaluate got last
    public void bindTo(IShader shader) {
        for (var e : values.object2FloatEntrySet()) {
            shader.getUniform(e.getKey()).set(e.getFloatValue());
        }
    }
}
