package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.mehvahdjukaar.polytone.utils.exp.IExpression;
import net.mehvahdjukaar.polytone.utils.exp.PolytoneExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BlockContextExpression extends PolytoneExpression {

    public static final Codec<BlockContextExpression> CODEC =
            CodecUtils.STR_OR_DOUBLE_CODEC.flatXmap(s -> {
        try {
            return DataResult.success(new BlockContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean hasState;

    public BlockContextExpression(String unparsed) {
        this(unparsed, false);
    }

    public BlockContextExpression(String unparsed, boolean concurrent) {
        super(unparsed, concurrent);
        this.hasState = unparsed.contains(STATE_FUNC);
    }

    @Override
    protected BlockContextExpression createConcurrent() {
        return new BlockContextExpression(this.getUnparsed(), true);
    }

    @Override
    protected void buildFunctions(FunBuilder builder) {
        super.buildFunctions(builder);
        builder.add(STATE_PROP);
        builder.add(STATE_PROP_INT);
    }

    //TODO: turn into entity context expression
    public double getValue(Vec3 pos, float entityTime, Level level) {
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
        if (hasRain) vb.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
        if (hasSeason) vb.setVariable(SEASON, ClientFrameTicker.getSeason());

        if (hasSkyLight) vb.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, p));
        if (hasBlockLight) vb.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, p));
        if (hasTemperature)
            vb.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(p).value()).temperature());
        if (hasDownfall)
            vb.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(p).value()).downfall());

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

    public double getValue(Level level, @NotNull BlockPos pos, BlockState state) {
        ExpressionUtils.seedRandom(state.getSeed(pos));

        IExpression.IVars vars = expression.varBuilder();

        if (hasPos) {
            vars.setVariable(POS_X, pos.getX());
            vars.setVariable(POS_Y, pos.getY());
            vars.setVariable(POS_Z, pos.getZ());
        }
        if (hasTime) vars.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) vars.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) vars.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) vars.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
        if (hasSeason) vars.setVariable(SEASON, ClientFrameTicker.getSeason());

        if (hasSkyLight) vars.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight) vars.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, pos));
        if (hasTemperature)
            vars.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(pos).value()).temperature());
        if (hasDownfall)
            vars.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(pos).value()).downfall());

        if (hasState) STATE_HACK.set(state);

        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            vars.setVariable(PLAYER_X, e.getX());
            vars.setVariable(PLAYER_Y, e.getY());
            vars.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            Entity e = Minecraft.getInstance().getCameraEntity();
            double x = pos.getX() - e.getX();
            double y = pos.getY() - e.getY();
            double z = pos.getZ() - e.getZ();
            vars.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            vars.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) vars.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());

        return expression.evaluate(vars);
    }

    public static final BlockContextExpression ZERO = new BlockContextExpression("0");
    public static final BlockContextExpression ONE = new BlockContextExpression("1");
    public static final BlockContextExpression PARTICLE_RAND = new BlockContextExpression("(rand() * 2.0 - 1.0) * 0.4");
}
