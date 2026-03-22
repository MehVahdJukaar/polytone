package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.colormap.ColormapExpressionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.mehvahdjukaar.polytone.common.ColorUtils.getClimateSettings;

public interface IColormapExp {

    MapRegistry<IColormapExp> BUILTIN_EXP = new MapRegistry<>("Colormap Number Providers");

    Codec<IColormapExp> CODEC = Codec.lazyInitialized(() -> CodecUtils.referenceOrDirect(BUILTIN_EXP,
            CodecUtils.alternatives(
                    Codec.FLOAT.xmap(
                            aDouble -> (a,b,c,d,e,f)
                                    -> aDouble,
                            iBlockExp -> 0.0f
                    ),
                    ColormapExpressionProvider.CODEC, ColormapExp.TYPE.codec()), true));

    float evaluate(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
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
        public float evaluate(@Nullable BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return biome == null ? 0 : getClimateSettings(biome).temperature;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapExp LEGACY_TEMPERATURE = BUILTIN_EXP.register("legacy_temperature", new IColormapExp() {
        @Override
        public float evaluate(@Nullable BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return (biome == null || pos == null) ? 0 : biome.getTemperature(BlockPos.containing(pos),
                    Minecraft.getInstance().level.getSeaLevel()); // TODO BAD!
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapExp DOWNFALL = BUILTIN_EXP.register("downfall", new IColormapExp() {
        @Override
        public float evaluate(@Nullable BlockAndTintGetter level, BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                              @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            return biome == null ? 0 : getClimateSettings(biome).downfall;
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
                public float evaluate(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
                    if (biome == null) return 0;
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
        public float evaluate(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
            if (pos == null) return 64;
            BlockPos bp = BlockPos.containing(pos);
            // some builtin variance just because
            // 0-128 RANGE. no clue what that darn mod did but this is good enough. People shouldn't use this anyway
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
        public float evaluate(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
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
        public float evaluate(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
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