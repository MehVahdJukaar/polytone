package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.exp.BaseExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public final class ColormapExpressionProvider extends BaseExpression implements IColormapNumberProvider {

    //Keywords
    private static final String BIOME_VALUE = "BIOME_VALUE";
    private static final String DAMAGE = "DAMAGE";

    public static final Codec<ColormapExpressionProvider> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new ColormapExpressionProvider(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    private final boolean usesBiome;
    private final boolean hasState;


    private ColormapExpressionProvider(String unparsed) {
        super(unparsed);

        this.usesBiome = unparsed.contains(BaseExpression.TEMPERATURE) || unparsed.contains(BaseExpression.DOWNFALL)
                || unparsed.contains(BIOME_VALUE);
        this.hasState = unparsed.contains(BaseExpression.STATE_FUNC);
    }

    @Override
    protected void buildVars(VarBuilder builder) {
        super.buildVars(builder);
        builder.addAll(BIOME_VALUE, DAMAGE);
    }

    @Override
    protected void buildFunctions(FunBuilder builder) {
        super.buildFunctions(builder);
        builder.addAll(STATE_PROP_INT, STATE_PROP);
    }

    @Override
    public boolean usesBiome() {
        return usesBiome;
    }

    @Override
    public boolean usesPos() {
        return this.hasPos;
    }

    @Override
    public boolean usesState() {
        return this.hasState;
    }

    @Override
    public float getValue(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                          @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {

        if (pos == null) {
            pos = BlockPos.ZERO;
            ExpressionUtils.randomizeRandom();
        } else {
            ExpressionUtils.seedRandom(pos.hashCode() * pos.asLong());
        }

        if (hasPos) {
            expression.setVariable(POS_X, pos.getX());
            expression.setVariable(POS_Y, pos.getY());
            expression.setVariable(POS_Z, pos.getZ());
        }

        if (hasTime) expression.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasDayTime) expression.setVariable(BaseExpression.DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSunTime) expression.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());

        if (hasSkyLight)
            expression.setVariable(SKY_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight)
            expression.setVariable(BLOCK_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.BLOCK, pos));
        if (hasTemperature)
            expression.setVariable(BaseExpression.TEMPERATURE, biome != null ? ColorUtils.getClimateSettings(biome).temperature : 0);
        if (hasDownfall)
            expression.setVariable(BaseExpression.DOWNFALL, biome != null ? ColorUtils.getClimateSettings(biome).downfall : 0);

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


        if (stack != null) {
            float damage = 1 - stack.getDamageValue() / (float) stack.getMaxDamage();
            expression.setVariable(DAMAGE, damage);
        } else expression.setVariable(DAMAGE, 0);

        // Evaluate the expressionression
        //this state hack won't even work as its multithreaded lmao

        if (hasState) STATE_HACK.set(state);

        float result = (float) expression.evaluate();
        STATE_HACK.remove();

        return result;
    }
}
