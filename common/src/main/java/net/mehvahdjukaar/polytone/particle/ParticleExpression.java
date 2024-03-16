package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
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

    private final boolean hasColor;
    private final boolean hasSpeed;
    private final boolean hasX;
    private final boolean hasY;
    private final boolean hasZ;
    private final boolean hasDx;
    private final boolean hasDy;
    private final boolean hasDz;
    private final boolean hasRed;
    private final boolean hasGreen;
    private final boolean hasBlue;
    private final boolean hasAlpha;
    private final boolean hasSize;
    private final boolean hasLifeTime;


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
        this.hasColor = unparsed.contains(COLOR);
        this.hasSpeed = unparsed.contains(SPEED);
        this.hasX = unparsed.contains(X);
        this.hasY = unparsed.contains(Y);
        this.hasZ = unparsed.contains(Z);
        this.hasDx = unparsed.contains(DX);
        this.hasDy = unparsed.contains(DY);
        this.hasDz = unparsed.contains(DZ);
        this.hasRed = unparsed.contains(RED);
        this.hasGreen = unparsed.contains(GREEN);
        this.hasBlue = unparsed.contains(BLUE);
        this.hasAlpha = unparsed.contains(ALPHA);
        this.hasSize = unparsed.contains(SIZE);
        this.hasLifeTime = unparsed.contains(LIFE);
    }

    public static ParticleExpression parse(String s) {
        return new ParticleExpression(createExpression(s), s);
    }

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(s)
                .functions(ExpressionUtils.defFunc())
                .variables(COLOR, SPEED, X, Y, Z, DX, DY, DZ, RED, GREEN, BLUE, ALPHA, SIZE, LIFE)
                .operator(ExpressionUtils.defOp())
                .build();
    }


    public double get(Particle particle, ParticleOptions options) {
        if (hasLifeTime) expression.setVariable(LIFE, particle.getLifetime());
        if (hasColor) {
            int pack = ColorUtils.pack(particle.rCol, particle.gCol, particle.bCol);
            expression.setVariable(COLOR, pack);

        }
        if (hasRed) expression.setVariable(RED, particle.rCol);
        if (hasGreen) expression.setVariable(GREEN, particle.gCol);
        if (hasBlue) expression.setVariable(BLUE, particle.bCol);
        if (hasSpeed) expression.setVariable(SPEED, Mth.length(particle.xd, particle.yd, particle.zd));
        if (hasAlpha) expression.setVariable(ALPHA, particle.alpha);
        if (hasSize) expression.setVariable(SIZE, particle.getBoundingBox().getSize());
        if (hasDx) expression.setVariable(DX, particle.xd);
        if (hasDy) expression.setVariable(DX, particle.yd);
        if (hasDz) expression.setVariable(DX, particle.zd);
        if (hasX) expression.setVariable(DX, particle.x);
        if (hasY) expression.setVariable(DX, particle.y);
        if (hasZ) expression.setVariable(DX, particle.z);

        return expression.evaluate();
    }

}
