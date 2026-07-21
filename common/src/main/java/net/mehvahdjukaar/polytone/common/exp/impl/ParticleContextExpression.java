package net.mehvahdjukaar.polytone.common.exp.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.exp.IExpression;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class ParticleContextExpression extends PolytoneExpression implements IParticleExp {

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
            // Custom particles tick across a worker pool when async is on, and every particle of a
            // type shares this one expression instance. The plain (ExpressionDelegate) backing keeps
            // its variables on a single shared exp4j Expression, so concurrent evaluation stomps them
            // and yields garbage (visible as particles randomly rotating/jittering). Build the
            // per-thread ConcurrentExpression instead. Only pay that when async is actually enabled;
            // toggling the config triggers a resource reload, so this is re-evaluated on each load.
            boolean concurrent = Polytone.CONFIGS.particlesOffThread.get();
            return DataResult.success(new ParticleContextExpression(s, concurrent));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean hasCustom;

    public ParticleContextExpression(String expression) {
        this(expression, false);
    }

    public ParticleContextExpression(String expression, boolean concurrent) {
        super(expression, concurrent);
        this.hasCustom = expression.contains(CUSTOM);
    }

    @Override
    protected ParticleContextExpression createConcurrent() {
        return new ParticleContextExpression(this.getUnparsed(), true);
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


    public double evaluate(Particle particle, Level level) {
        IExpression.IVars vb = expression.varBuilder();
        vb.setVariable(LIFE, particle.getLifetime());
        if (particle instanceof SingleQuadParticle sqp) {

            int pack = ColorUtils.pack(sqp.rCol, sqp.gCol, sqp.bCol);
            vb.setVariable(COLOR, pack);

            vb.setVariable(RED, sqp.rCol);
            vb.setVariable(GREEN, sqp.gCol);
            vb.setVariable(BLUE, sqp.bCol);
            vb.setVariable(ALPHA, sqp.alpha);
            vb.setVariable(SIZE, sqp.quadSize);
            vb.setVariable(ROLL, sqp.roll);
        }
        vb.setVariable(SPEED, Mth.length(particle.xd, particle.yd, particle.zd));
        vb.setVariable(DX, particle.xd);
        vb.setVariable(DY, particle.yd);
        vb.setVariable(DZ, particle.zd);
        vb.setVariable(X, particle.x);
        vb.setVariable(Y, particle.y);
        vb.setVariable(Z, particle.z);
        vb.setVariable(AGE, particle.age);
        if (hasCustom && particle instanceof CustomParticleInstance i)
            vb.setVariable(CUSTOM, i.getCustom());


        BlockPos pos = BlockPos.containing(particle.x, particle.y, particle.z);
        if (hasPos) {
            vb.setVariable(POS_X, pos.getX());
            vb.setVariable(POS_Y, pos.getY());
            vb.setVariable(POS_Z, pos.getZ());
        }

        if (hasTime) vb.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) vb.setVariable(PolytoneExpression.DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) vb.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) vb.setVariable(RAIN, ExpTicker.getRainAndThunder());
        if (hasSeason) vb.setVariable(PolytoneExpression.SEASON, ExpTicker.getSeasonNumber());

        if (hasSkyLight)
            vb.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight)
            vb.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, pos));
        if (hasTemperature)
            vb.setVariable(PolytoneExpression.TEMPERATURE, ClientFrameTicker.getTemperature());
        if (hasDownfall)
            vb.setVariable(PolytoneExpression.DOWNFALL, ClientFrameTicker.getDownfall());


        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            if (e != null) {

                vb.setVariable(PLAYER_X, e.getX());
                vb.setVariable(PLAYER_Y, e.getY());
                vb.setVariable(PLAYER_Z, e.getZ());
            } else {
                vb.setVariable(PLAYER_X, 0);
                vb.setVariable(PLAYER_Y, 0);
                vb.setVariable(PLAYER_Z, 0);
            }
        }
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            if (e != null) {
                double x = particle.x - e.getX();
                double y = particle.y - e.getY();
                double z = particle.z - e.getZ();
                vb.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
            } else {
                vb.setVariable(DISTANCE_SQUARED, 0);
            }
        }
        if (hasPlayerSpeed) {
            vb.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) vb.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());


        ExpressionUtils.randomizeRandom();
        return expression.evaluate(vb);
    }

}
