package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IColormapModExp;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.mehvahdjukaar.polytone.utils.exp.IExpression;
import net.mehvahdjukaar.polytone.utils.exp.PolytoneExpression;
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

import java.util.Optional;

public class ColormapColorModulatorExpression {

    public static Codec<ColormapColorModulatorExpression> CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    IColormapModExp.CODEC.optionalFieldOf("red").forGetter(c -> c.red),
                    IColormapModExp.CODEC.optionalFieldOf("green").forGetter(c -> c.green),
                    IColormapModExp.CODEC.optionalFieldOf("blue").forGetter(c -> c.blue)
            ).apply(i, ColormapColorModulatorExpression::new));

    private final Optional<IColormapModExp> red;
    private final Optional<IColormapModExp> green;
    private final Optional<IColormapModExp> blue;

    protected ColormapColorModulatorExpression(Optional<IColormapModExp> red, Optional<IColormapModExp> green, Optional<IColormapModExp> blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public ColormapColorModulatorExpression createConcurrent() {
        return new ColormapColorModulatorExpression(
                red.map(IColormapModExp::createConcurrent),
                green.map(IColormapModExp::createConcurrent),
                blue.map(IColormapModExp::createConcurrent)
        );
    }

    public int getValue(int original, @Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        float[] values = ColorUtils.unpack(original);
        float red = values[0];
        float green = values[1];
        float blue = values[2];

        Vec3 vPos = pos == null ? null : pos.getCenter();
        float newRed = this.red.map(exp -> exp.evaluate(red, green, blue, null, state, vPos, biome, mapper, stack)).orElse(red);
        float newGreen = this.green.map(exp -> exp.evaluate(red, green, blue, null, state, vPos, biome, mapper, stack)).orElse(green);
        float newBlue = this.blue.map(exp -> exp.evaluate(red, green, blue, null, state, vPos, biome, mapper, stack)).orElse(blue);
        return ColorUtils.pack(newRed, newGreen, newBlue);
    }

    public static class Exp extends ColormapExpressionProvider implements IColormapModExp {

        private static final String RED = "RED";
        private static final String GREEN = "GREEN";
        private static final String BLUE = "BLUE";
        private static final String ALPHA = "ALPHA";

        public static final Codec<Exp> CODEC = CodecUtils.STR_OR_DOUBLE_CODEC.flatXmap(s -> {
            try {
                return DataResult.success(new Exp(s));
            } catch (Exception e) {
                return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
            }
        }, javaxExpression -> DataResult.success(javaxExpression.getUnparsed()));

        protected Exp(String unparsed) {
            super(unparsed);
        }

        protected Exp(String unparsed, boolean concurrent) {
            super(unparsed, concurrent);
        }


        @Override
        public float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level,
                              @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return getValue(r, g, b, state, pos == null ? null : BlockPos.containing(pos), biome, mapper, stack);
        }

        public float getValue(float r, float g, float b, @Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            //mega ugly

            if (pos == null) {
                pos = BlockPos.ZERO;
                ExpressionUtils.randomizeRandom();
            } else {
                ExpressionUtils.seedRandom(pos.hashCode() * pos.asLong());
            }
            IExpression.IVars vb = expression.varBuilder();

            vb.setVariable(RED, r);
            vb.setVariable(GREEN, g);
            vb.setVariable(BLUE, b);

            if (hasPos) {
                vb.setVariable(POS_X, pos.getX());
                vb.setVariable(POS_Y, pos.getY());
                vb.setVariable(POS_Z, pos.getZ());
            }

            if (hasTime) vb.setVariable(TIME, ClientFrameTicker.getGameTime());
            if (hasDayTime) vb.setVariable(PolytoneExpression.DAY_TIME, ClientFrameTicker.getDayTime());
            if (hasSunTime) vb.setVariable(SUN_TIME, ClientFrameTicker.getSunTime());
            if (hasRain) vb.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
            if (hasSeason) vb.setVariable(PolytoneExpression.SEASON, ClientFrameTicker.getSeason());

            if (hasSkyLight)
                vb.setVariable(SKY_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.SKY, pos));
            if (hasBlockLight)
                vb.setVariable(BLOCK_LIGHT, Minecraft.getInstance().level.getBrightness(LightLayer.BLOCK, pos));
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
                double x = pos.getX() - e.getX();
                double y = pos.getY() - e.getY();
                double z = pos.getZ() - e.getZ();
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

            //this state hack won't even work as its multithreaded lmao

            if (hasState) STATE_HACK.set(state);

            float result = (float) expression.evaluate(vb);
            STATE_HACK.remove();

            return result;
        }

        @Override
        public Exp createConcurrent() {
            return new Exp(this.getUnparsed(), true);
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
}