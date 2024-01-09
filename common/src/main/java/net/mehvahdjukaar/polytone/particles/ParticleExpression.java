package net.mehvahdjukaar.polytone.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.client.particle.Particle;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import static net.mehvahdjukaar.polytone.colormap.ExpressionSource.*;

public class ParticleExpression {
    private final Expression expression;
    private final String unparsed;

    private static final String ORIGINAL = "ORIGINAL";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String DX = "DX";
    private static final String DY = "DY";
    private static final String DZ = "DZ";
    private static final String RED = "RED";
    private static final String GREEN = "GREEN";
    private static final String BLUE = "BLUE";
    private static final String ALPHA = "ALPHA";
    private static final String SIZE = "SIZE";


    public static final Codec<ParticleExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled = createExpression(s);
            return DataResult.success(new ParticleExpression(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));


    public ParticleExpression(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
    }

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(s)
                .functions(ExpressionUtils.defFunc())
                .variables(ORIGINAL, X, Y, Z, DX, DY, DZ, RED, GREEN, BLUE, ALPHA, SIZE)
                .operator(ExpressionUtils.defOp())
                .build();
    }

    public static ParticleExpression parse(String input) {

        return null;
    }


    public double get(Particle particle) {


        return expression.evaluate();
    }

}
