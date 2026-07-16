package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.content.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IColormapExp {

    MapRegistry<IColormapExp> BUILTIN_EXP = new MapRegistry<>("Colormap Number Providers");

    // named builtin | constant | legacy exp4j expression (ColormapExpressionProvider) | MVEL expression (ColormapExp)
    Codec<IColormapExp> CODEC = Codec.lazyInitialized(() -> CodecUtils.referenceOrDirect(BUILTIN_EXP,
            CodecUtils.alternatives(
                    CodecUtils.LENIENT_FLOAT.xmap(
                            aDouble -> (IColormapExp) (a, b, c, d, e, f) -> aDouble,
                            i -> 0.0f
                    ),
                    ColormapExpressionProvider.CODEC,
                    ColormapExp.TYPE.codec()), true));

    float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                   @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack);

    default boolean usesBiome() {
        return true;
    }

    default boolean usesPos() {
        return true;
    }

    default boolean usesState() {
        return true;
    }

    default IColormapExp createConcurrent() {
        return this;
    }

    record Const(float c) implements IColormapExp {

        @Override
        public float evaluate(@Nullable BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return c;
        }

        @Override
        public boolean usesState() {
            return false;
        }

        @Override
        public boolean usesBiome() {
            return false;
        }

        @Override
        public boolean usesPos() {
            return false;
        }
    }

    IColormapExp ZERO = BUILTIN_EXP.register("zero", new Const(0));
    IColormapExp ONE = BUILTIN_EXP.register("one", new Const(1));

    //why inverted. for sunset colormaps
    IColormapExp DAY_TIME = BUILTIN_EXP.register("day_time", (level, state, pos, biome, mapper, stack) ->
            (float) (1f - (ClientFrameTicker.getDayTime() % 24000 / 24000f)));


    IColormapExp TEMPERATURE = BUILTIN_EXP.register("temperature", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return biome == null ? 0 : ColorUtils.getClimateSettings(biome).temperature();
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapExp LEGACY_TEMPERATURE = BUILTIN_EXP.register("legacy_temperature", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            if (biome == null || pos == null) return 0;
            // 1.21.1: Biome.getTemperature(BlockPos) takes only BlockPos (no sea level param)
            return biome.getTemperature(BlockPos.containing(pos));
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapExp DOWNFALL = BUILTIN_EXP.register("downfall", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return biome == null ? 0 : ColorUtils.getClimateSettings(biome).downfall();
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    // grid format
    IColormapExp BIOME_ID = BUILTIN_EXP.register("biome_id",
            new IColormapExp() {
                @Override
                public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
                    if (biome == null || mapper == null) return 0;
                    return 1 - mapper.getIndex(biome);
                }

                @Override
                public boolean usesState() {
                    return false;
                }
            }
    );


    IColormapExp Y_LEVEL = BUILTIN_EXP.register("y_level", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            if (pos == null) return 64;
            BlockPos bp = BlockPos.containing(pos);
            // 0-128 RANGE
            RandomSource rs = RandomSource.create(pos.hashCode() * bp.asLong());
            float yVariance = 4;
            float v = yVariance * (rs.nextFloat() - 0.5f);
            return (float) (1 - ((pos.y() + 64 + v) / 256f));
        }

        @Override
        public boolean usesBiome() {
            return false;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });


    IColormapExp DAMAGE = BUILTIN_EXP.register("item_damage", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            if (stack == null) return 0;
            return 1 - stack.getDamageValue() / (float) stack.getMaxDamage();
        }

        @Override
        public boolean usesBiome() {
            return false;
        }

        @Override
        public boolean usesPos() {
            return false;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapExp SEASON = BUILTIN_EXP.register("season", new IColormapExp() {
        @Override
        public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return 1 - ExpTicker.getSeasonNumber();
        }

        @Override
        public boolean usesBiome() {
            return false;
        }

        @Override
        public boolean usesPos() {
            return false;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });
}
