package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class ParticleContextExpression {
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
    private static final String AGE = "AGE";
    private static final String ROLL = "ROLL";
    private static final String CUSTOM = "CUSTOM";

    private static final String DAY_TIME = "DAY_TIME";
    private static final String TIME = "TIME";
    private static final String RAIN = "RAIN";


    public static final Codec<ParticleContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled = createExpression(s);
            return DataResult.success(new ParticleContextExpression(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private final boolean hasTime;
    private final boolean hasRain;
    private final boolean hasDayTime;
    private final boolean hasCustom;


    public ParticleContextExpression(String expression) {
        this(createExpression(expression), expression);
    }

    public ParticleContextExpression(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;

        this.hasTime = unparsed.contains(TIME);
        this.hasRain = unparsed.contains(RAIN);
        this.hasDayTime = unparsed.contains(DAY_TIME);
        this.hasCustom = unparsed.contains(CUSTOM);
    }

    public static ParticleContextExpression parse(String s) {
        return new ParticleContextExpression(createExpression(s), s);
    }

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(s)
                .functions(ExpressionUtils.defFunc())
                .variables(COLOR, SPEED, X, Y, Z, DX, DY, DZ, RED, GREEN, BLUE, ALPHA, SIZE, LIFE, ROLL, AGE,
                        CUSTOM, TIME, RAIN, DAY_TIME)
                .operator(ExpressionUtils.defOp())
                .build();
    }


    public double getValue(Particle particle, Level level) {
        expression.setVariable(LIFE, particle.getLifetime());

        int pack = ColorUtils.pack(particle.rCol, particle.gCol, particle.bCol);
        expression.setVariable(COLOR, pack);

        expression.setVariable(RED, particle.rCol);
        expression.setVariable(GREEN, particle.gCol);
        expression.setVariable(BLUE, particle.bCol);
        expression.setVariable(SPEED, Mth.length(particle.xd, particle.yd, particle.zd));
        expression.setVariable(ALPHA, particle.alpha);
        expression.setVariable(SIZE, ((SingleQuadParticle) particle).quadSize);
        expression.setVariable(DX, particle.xd);
        expression.setVariable(DX, particle.yd);
        expression.setVariable(DX, particle.zd);
        expression.setVariable(DX, particle.x);
        expression.setVariable(DX, particle.y);
        expression.setVariable(DX, particle.z);
        expression.setVariable(AGE, particle.age);
        expression.setVariable(ROLL, particle.roll);
        if (hasCustom && particle instanceof CustomParticleType.Instance i)
            expression.setVariable(CUSTOM, i.getCustom());

        if (hasTime) expression.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
        if (hasDayTime) expression.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());

        ExpressionUtils.randomizeRandom();
        return expression.evaluate();
    }

    public static final ParticleContextExpression ZERO = new ParticleContextExpression("0");
    public static final ParticleContextExpression ONE = new ParticleContextExpression("1");
    public static final ParticleContextExpression PARTICLE_RAND= new ParticleContextExpression("(rand() * 2.0 - 1.0) * 0.4");
}
