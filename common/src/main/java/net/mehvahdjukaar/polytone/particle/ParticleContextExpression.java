package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.exp.BaseExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class ParticleContextExpression extends BaseExpression {

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

    public static final Codec<ParticleContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new ParticleContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean hasCustom;

    public ParticleContextExpression(String expression) {
        super(expression);
        this.hasCustom = expression.contains(CUSTOM);
    }

    @Override
    protected void buildFunctions(FunBuilder builder) {
        super.buildFunctions(builder);
    }

    @Override
    protected void buildVars(VarBuilder builder) {
        super.buildVars(builder);
        builder.addAll(COLOR, SPEED, X, Y, Z, DX, DY, DZ, RED, GREEN, BLUE, ALPHA, SIZE, LIFE, AGE, ROLL, CUSTOM);
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
        expression.setVariable(DY, particle.yd);
        expression.setVariable(DZ, particle.zd);
        expression.setVariable(X, particle.x);
        expression.setVariable(Y, particle.y);
        expression.setVariable(Z, particle.z);
        expression.setVariable(AGE, particle.age);
        expression.setVariable(ROLL, particle.roll);
        if (hasCustom && particle instanceof CustomParticleType.Instance i)
            expression.setVariable(CUSTOM, i.getCustom());



        if (hasPos) {
            BlockPos pos = BlockPos.containing(particle.x, particle.y, particle.z);
            expression.setVariable(POS_X, pos.getX());
            expression.setVariable(POS_Y, pos.getY());
            expression.setVariable(POS_Z, pos.getZ());
        }

        if (hasTime) expression.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) expression.setVariable(BaseExpression.DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) expression.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());

        if (hasSkyLight)
            expression.setVariable(SKY_LIGHT, ClientFrameTicker.getSkyLight());
        if (hasBlockLight)
            expression.setVariable(BLOCK_LIGHT, ClientFrameTicker.getBlockLight());
        if (hasTemperature)
            expression.setVariable(BaseExpression.TEMPERATURE, ClientFrameTicker.getTemperature());
        if (hasDownfall)
            expression.setVariable(BaseExpression.DOWNFALL, ClientFrameTicker.getDownfall());


        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            expression.setVariable(PLAYER_X, e.getX());
            expression.setVariable(PLAYER_Y, e.getY());
            expression.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            double x = particle.x - e.getX();
            double y = particle.y - e.getY();
            double z = particle.z - e.getZ();
            expression.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            expression.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) expression.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());


        ExpressionUtils.randomizeRandom();
        return expression.evaluate();
    }

    public static final ParticleContextExpression ZERO = new ParticleContextExpression("0");
    public static final ParticleContextExpression ONE = new ParticleContextExpression("1");
    public static final ParticleContextExpression PARTICLE_RAND = new ParticleContextExpression("(rand() * 2.0 - 1.0) * 0.4");
}
