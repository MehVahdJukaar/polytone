package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.mehvahdjukaar.polytone.utils.ColorUtils.getClimateSettings;

public interface IColormapNumberProvider {

    BiMap<String, IColormapNumberProvider> CUSTOM_PROVIDERS = HashBiMap.create();

    Codec<IColormapNumberProvider> REFERENCE_CODEC = ExtraCodecs.stringResolverCodec(
            a -> CUSTOM_PROVIDERS.inverse().get(a), CUSTOM_PROVIDERS::get);

    Codec<IColormapNumberProvider> CODEC = new ReferenceOrDirectCodec<>(REFERENCE_CODEC,
            ColormapExpressionProvider.CODEC, true);

    static <T extends IColormapNumberProvider> T register(String name, T provider) {
        CUSTOM_PROVIDERS.put(name, provider);
        return provider;
    }

    float getValue(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome);

    default boolean usesBiome() {
        return true;
    }

    default boolean usesPos() {
        return true;
    }

    default boolean usesState() {
        return true;
    }

    record Const(float c) implements IColormapNumberProvider {

        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
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

    IColormapNumberProvider ZERO = register("zero", new Const(0));
    IColormapNumberProvider ONE = register("one", new Const(1));

    IColormapNumberProvider TEMPERATURE = register("temperature", new IColormapNumberProvider() {
        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
            return biome == null ? 0 : getClimateSettings(biome).temperature;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapNumberProvider LEGACY_TEMPERATURE = register("legacy_temperature", new IColormapNumberProvider() {
        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
            return biome == null ? 0 : biome.getTemperature(pos);
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapNumberProvider DOWNFALL = register("downfall", new IColormapNumberProvider() {
        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
            return biome == null ? 0 : getClimateSettings(biome).downfall;
        }

        @Override
        public boolean usesState() {
            return false;
        }
    });

    IColormapNumberProvider BIOME_ID = register("biome_id", (state, pos, b) -> Minecraft.getInstance().level
            .registryAccess().registry(Registries.BIOME).get().getId(b) / 256f);

    IColormapNumberProvider Y_LEVEL = register("y_level", (state, pos, biome) ->
            (pos == null ? 64 : pos.getY()) / 256f); //hoping this will be flipped

}