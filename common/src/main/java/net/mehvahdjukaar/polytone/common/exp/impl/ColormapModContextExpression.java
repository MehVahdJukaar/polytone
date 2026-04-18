package net.mehvahdjukaar.polytone.common.exp.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.exp.IExpression;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapModExp;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.colormap.ColormapExpressionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ColormapModContextExpression extends ColormapExpressionProvider implements IColormapModExp {

    private static final String RED = "RED";
    private static final String GREEN = "GREEN";
    private static final String BLUE = "BLUE";
    private static final String ALPHA = "ALPHA";

    public static final Codec<ColormapModContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new ColormapModContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    protected ColormapModContextExpression(String unparsed) {
        super(unparsed);
    }

    protected ColormapModContextExpression(String unparsed, boolean concurrent) {
        super(unparsed, concurrent);
    }


    public float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level, @Nullable BlockState state,
                          @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        //mega ugly

        BlockPos bp = pos == null ? BlockPos.ZERO : BlockPos.containing(pos);
        if (pos == null) {
            pos = Vec3.ZERO;
            ExpressionUtils.randomizeRandom();
        } else {
            ExpressionUtils.seedRandom(pos.hashCode() * bp.asLong());
        }
        IExpression.IVars vb = expression.varBuilder();

        vb.setVariable(RED, r);
        vb.setVariable(GREEN, g);
        vb.setVariable(BLUE, b);

        if (hasPos) {
            vb.setVariable(POS_X, pos.x());
            vb.setVariable(POS_Y, pos.y());
            vb.setVariable(POS_Z, pos.z());
        }

        if (hasTime) vb.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) vb.setVariable(PolytoneExpression.DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) vb.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) vb.setVariable(RAIN, ExpTicker.getRainAndThunder());
        if (hasSeason) vb.setVariable(PolytoneExpression.SEASON, ExpTicker.getSeasonNumber());

        if (hasSkyLight)
            vb.setVariable(SKY_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.SKY, bp));
        if (hasBlockLight)
            vb.setVariable(BLOCK_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.BLOCK, bp));
        if (hasTemperature)
            vb.setVariable(PolytoneExpression.TEMPERATURE, biome != null ? ColorUtils.getClimateSettings(biome).temperature() : 0);
        if (hasDownfall)
            vb.setVariable(PolytoneExpression.DOWNFALL, biome != null ? ColorUtils.getClimateSettings(biome).downfall() : 0);

        if (hasPlayer) {
            var e = Minecraft.getInstance().getCameraEntity();
            vb.setVariable(PLAYER_X, e.getX());
            vb.setVariable(PLAYER_Y, e.getY());
            vb.setVariable(PLAYER_Z, e.getZ());
        }
        if (hasDistance) {
            Entity e = Minecraft.getInstance().getCameraEntity();
            double x = pos.x() - e.getX();
            double y = pos.y() - e.getY();
            double z = pos.z() - e.getZ();
            vb.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        if (hasPlayerSpeed) {
            vb.setVariable(PLAYER_SPEED_SQUARED, ClientFrameTicker.getPlayerSpeed());
        }

        if (hasRenderDistance) vb.setVariable(RENDER_DISTANCE, ClientFrameTicker.getRenderDistance());


        if (stack != null) {
            float damage = 1 - stack.getDamageValue() / (float) stack.getMaxDamage();
            vb.setVariable(DAMAGE, damage);
        } else vb.setVariable(DAMAGE, 0);

        // Evaluate the expressionression
        //this state hack won't even work as its multithreaded lmao

        if (hasState) STATE_HACK.set(state);

        float result = (float) expression.evaluate(vb);
        STATE_HACK.remove();

        return result;
    }

    @Override
    public ColormapModContextExpression createConcurrent() {
        return new ColormapModContextExpression(this.getUnparsed(), true);
    }

    @Override
    protected void buildVars(VarBuilder builder) {
        super.buildVars(builder);
        builder.add(RED);
        builder.add(GREEN);
        builder.add(BLUE);
        builder.add(ALPHA);
    }
}

