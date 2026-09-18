package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapExp;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.exp.IExpression;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ColormapExpressionProvider extends PolytoneExpression implements IColormapExp {

    //Keywords
    protected static final String BIOME_VALUE = "BIOME_VALUE";
    protected static final String DAMAGE = "DAMAGE";

    public static final Codec<ColormapExpressionProvider> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new ColormapExpressionProvider(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

    protected final boolean usesBiome;
    protected final boolean hasState;

    protected ColormapExpressionProvider(String unparsed) {
        this(unparsed, false);
    }

    protected ColormapExpressionProvider(String unparsed, boolean concurrent) {
        super(unparsed, concurrent);

        this.usesBiome = unparsed.contains(PolytoneExpression.TEMPERATURE) || unparsed.contains(PolytoneExpression.DOWNFALL)
                || unparsed.contains(BIOME_VALUE);
        this.hasState = unparsed.contains(PolytoneExpression.STATE_FUNC);
    }

    @Override
    public ColormapExpressionProvider createConcurrent() {
        return new ColormapExpressionProvider(this.getUnparsed(), true);
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
    public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                          @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {

        BlockPos bp = pos == null ? BlockPos.ZERO : BlockPos.containing(pos);
        if (pos == null) {
            pos = Vec3.ZERO;
            ExpressionUtils.randomizeRandom();
        } else {
            ExpressionUtils.seedRandom(pos.hashCode() * bp.asLong());
        }
        IExpression.IVars vb = expression.varBuilder();

        if (hasPos) {
            vb.setVariable(POS_X, pos.x());
            vb.setVariable(POS_Y, pos.y());
            vb.setVariable(POS_Z, pos.z());
        }
        if(mapper != null && biome != null) {
            vb.setVariable(BIOME_VALUE, 1 - mapper.getIndex(biome));
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
            if (e != null) {
                vb.setVariable(PLAYER_X, e.getX());
                vb.setVariable(PLAYER_Y, e.getY());
                vb.setVariable(PLAYER_Z, e.getZ());
            }else{
                vb.setVariable(PLAYER_X, 0);
                vb.setVariable(PLAYER_Y, 0);
                vb.setVariable(PLAYER_Z, 0);
            }
        }
        if (hasDistance) {
            Entity e = Minecraft.getInstance().getCameraEntity();
            if (e != null) {
                double x = pos.x() - e.getX();
                double y = pos.y() - e.getY();
                double z = pos.z() - e.getZ();
                vb.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
            }else{
                vb.setVariable(DISTANCE_SQUARED, 0);
            }
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
}
