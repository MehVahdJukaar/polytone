package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.exp.BaseExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BlockContextExpression extends BaseExpression {

    public static final Codec<BlockContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new BlockContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean hasState;

    public BlockContextExpression(String unparsed) {
        super(unparsed);
        this.hasState = unparsed.contains(STATE_FUNC);
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

        if (hasPos) {
            expression.setVariable(POS_X, pos.x);
            expression.setVariable(POS_Y, pos.y);
            expression.setVariable(POS_Z, pos.z);
        }
        BlockPos p = BlockPos.containing(pos);

        if (hasTime) expression.setVariable(TIME, entityTime);
        if (hasDayTime) expression.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) expression.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());

        if (hasSkyLight) expression.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, p));
        if (hasBlockLight) expression.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, p));
        if (hasTemperature)
            expression.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(p).value()).temperature);
        if (hasDownfall)
            expression.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(p).value()).downfall);

        if (hasState) STATE_HACK.set(level.getBlockState(p));

        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            expression.setVariable(PLAYER_X, e.getX());
            expression.setVariable(PLAYER_Y, e.getY());
            expression.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            double x = pos.x - e.getX();
            double y = pos.y - e.getY();
            double z = pos.z - e.getZ();
            expression.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            expression.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) expression.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());
        return expression.evaluate();
    }

    public double getValue(Level level, @NotNull BlockPos pos, BlockState state) {
        ExpressionUtils.seedRandom(pos.hashCode() * pos.asLong());

        if (hasPos) {
            expression.setVariable(POS_X, pos.getX());
            expression.setVariable(POS_Y, pos.getY());
            expression.setVariable(POS_Z, pos.getZ());
        }
        if (hasTime) expression.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) expression.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) expression.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());

        if (hasSkyLight) expression.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight) expression.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, pos));
        if (hasTemperature)
            expression.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(pos).value()).temperature);
        if (hasDownfall)
            expression.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(pos).value()).downfall);

        if (hasState) STATE_HACK.set(state);

        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            expression.setVariable(PLAYER_X, e.getX());
            expression.setVariable(PLAYER_Y, e.getY());
            expression.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            double x = pos.getX() - e.getX();
            double y = pos.getY() - e.getY();
            double z = pos.getZ() - e.getZ();
            expression.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            expression.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) expression.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());

        return expression.evaluate();
    }

    public static final BlockContextExpression ZERO = new BlockContextExpression("0");
    public static final BlockContextExpression ONE = new BlockContextExpression("1");
    public static final BlockContextExpression PARTICLE_RAND = new BlockContextExpression("(rand() * 2.0 - 1.0) * 0.4");
}
