package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class ParticleExpression {
    private final Expression expression;
    private final String unparsed;

    private static final String COLOR = "COLOR";
    private static final String SPEED = "SPEED";
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
    private static final String LIFE = "LIFETIME";
    private static final String GAMETIME = "GAMETIME";


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

    public static ParticleExpression parse(String s) {
        return new ParticleExpression(createExpression(s), s);
    }

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(s)
                .functions(ExpressionUtils.defFunc())
                .variables(COLOR, SPEED, X, Y, Z, DX, DY, DZ, RED, GREEN, BLUE, ALPHA, SIZE, LIFE, GAMETIME)
                .operator(ExpressionUtils.defOp())
                .build();
    }


    public double get(Particle particle, Level level, ParticleOptions options) {
        expression.setVariable(LIFE, particle.getLifetime());

        int pack = ColorUtils.pack(particle.rCol, particle.gCol, particle.bCol);
        expression.setVariable(COLOR, pack);

        expression.setVariable(RED, particle.rCol);
        expression.setVariable(GREEN, particle.gCol);
        expression.setVariable(BLUE, particle.bCol);
        expression.setVariable(SPEED, Mth.length(particle.xd, particle.yd, particle.zd));
        expression.setVariable(ALPHA, particle.alpha);
        expression.setVariable(SIZE, particle.getBoundingBox().getSize());
        expression.setVariable(DX, particle.xd);
        expression.setVariable(DX, particle.yd);
        expression.setVariable(DX, particle.zd);
        expression.setVariable(DX, particle.x);
        expression.setVariable(DX, particle.y);
        expression.setVariable(DX, particle.z);
        expression.setVariable(GAMETIME, level.getGameTime());

        return expression.evaluate();
    }

}
