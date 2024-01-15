package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome);

    default boolean usesBiome() {
        return false;
    }

    IColormapNumberProvider ZERO = register("zero", (state, pos, b) -> 0);
    IColormapNumberProvider ONE = register("one", (state, pos, b) -> 1);

    IColormapNumberProvider TEMPERATURE = register("temperature", new IColormapNumberProvider() {
        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
            return biome == null ? 0 : biome.climateSettings.temperature;
        }

        @Override
        public boolean usesBiome() {
            return true;
        }
    });


    IColormapNumberProvider DOWNFALL = register("downfall", new IColormapNumberProvider() {
        @Override
        public float getValue(BlockState state, @NotNull BlockPos pos, @Nullable Biome biome) {
            return biome == null ? 0 : biome.climateSettings.downfall;
        }

        @Override
        public boolean usesBiome() {
            return true;
        }
    });

    IColormapNumberProvider BIOME_ID = register("biome_id", (state, pos, b) -> Minecraft.getInstance().level
            .registryAccess().registry(Registries.BIOME).get().getId(b) / 256f);

}