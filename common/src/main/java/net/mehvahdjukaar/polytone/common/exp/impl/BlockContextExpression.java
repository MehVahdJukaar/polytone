package net.mehvahdjukaar.polytone.common.exp.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.exp.IExpression;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockContextExpression extends PolytoneExpression implements IBlockExp {

    public static final Codec<BlockContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new BlockContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    // bound by the expression-driven model selector to its "selector" value; 0 otherwise
    private static final String V = "v";

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

    @Override
    protected void buildVars(VarBuilder builder) {
        super.buildVars(builder);
        builder.add(V);
    }

    @Override
    public double evaluate(LevelReader level, @NotNull Vec3 p, @Nullable BlockState state) {
        return evaluate(level, p, state, 0);
    }

    // Variant that binds v, the value of a model selector's "selector" expression
    public double evaluate(LevelReader level, @NotNull Vec3 p, @Nullable BlockState state, double v) {
        BlockPos pos = BlockPos.containing(p);
        ExpressionUtils.seedRandom(state == null ? 42 : state.getSeed(pos));

        IExpression.IVars vars = expression.varBuilder();

        if (hasPos) {
            vars.setVariable(POS_X, pos.getX());
            vars.setVariable(POS_Y, pos.getY());
            vars.setVariable(POS_Z, pos.getZ());
        }
        if (hasTime) vars.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) vars.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) vars.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) vars.setVariable(RAIN, ExpTicker.getRainAndThunder());
        if (hasSeason) vars.setVariable(SEASON, ExpTicker.getSeasonNumber());

        if (hasSkyLight) vars.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight) vars.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, pos));
        if (hasTemperature)
            vars.setVariable(TEMPERATURE, ColorUtils.getClimateSettings(level.getBiome(pos).value()).temperature());
        if (hasDownfall)
            vars.setVariable(DOWNFALL, ColorUtils.getClimateSettings(level.getBiome(pos).value()).downfall());

        if (hasState && state != null) STATE_HACK.set(state);

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

        vars.setVariable(V, v);

        return expression.evaluate(vars);
    }
}
