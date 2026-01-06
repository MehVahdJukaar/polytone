package net.mehvahdjukaar.polytone.common.exp.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.exp.IExpression;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class EntityContextExpression extends PolytoneExpression  {

    public static final Codec<EntityContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new EntityContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean hasState;

    public EntityContextExpression(String unparsed) {
        this(unparsed, false);
    }

    public EntityContextExpression(String unparsed, boolean concurrent) {
        super(unparsed, concurrent);
        this.hasState = unparsed.contains(STATE_FUNC);
    }

    @Override
    protected EntityContextExpression createConcurrent() {
        return new EntityContextExpression(this.getUnparsed(), true);
    }

    @Override
    protected void buildFunctions(FunBuilder builder) {
        super.buildFunctions(builder);
        builder.add(STATE_PROP);
        builder.add(STATE_PROP_INT);
    }

    public double evaluate(Vec3 pos, float entityTime, Level level) {
        ExpressionUtils.randomizeRandom();

        IExpression.IVars vb = expression.varBuilder();

        if (hasPos) {
            vb.setVariable(POS_X, pos.x);
            vb.setVariable(POS_Y, pos.y);
            vb.setVariable(POS_Z, pos.z);
        }
        BlockPos p = BlockPos.containing(pos);

        if (hasTime) vb.setVariable(TIME, entityTime);
        if (hasDayTime) vb.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) vb.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) vb.setVariable(RAIN, ExpTicker.getRainAndThunder());
        if (hasSeason) vb.setVariable(SEASON, ExpTicker.getSeasonNumber());

        if (hasSkyLight) vb.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, p));
        if (hasBlockLight) vb.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, p));
        if (hasTemperature)
            vb.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(p).value()).temperature);
        if (hasDownfall)
            vb.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(p).value()).downfall);

        if (hasState) STATE_HACK.set(level.getBlockState(p));

        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            vb.setVariable(PLAYER_X, e.getX());
            vb.setVariable(PLAYER_Y, e.getY());
            vb.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            double x = pos.x - e.getX();
            double y = pos.y - e.getY();
            double z = pos.z - e.getZ();
            vb.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            vb.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) vb.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());
        return expression.evaluate(vb);
    }
}
