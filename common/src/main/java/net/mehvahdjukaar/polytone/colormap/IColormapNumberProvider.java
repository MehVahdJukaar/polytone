package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

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

    float getValue(BlockState state, @NotNull BlockAndTintGetter level, @NotNull BlockPos pos);

    IColormapNumberProvider ZERO = register("zero", (state, level, pos) -> 0);

    IColormapNumberProvider TEMPERATURE = register("temperature", (state, level, pos) ->
            level.getBlockTint(pos, TintMap.TEMPERATURE_RESOLVER));

    IColormapNumberProvider DOWNFALL = register("downfall", (state, level, pos) ->
            level.getBlockTint(pos, TintMap.DOWNFALL_RESOLVER));

    IColormapNumberProvider BIOME_ID = register("biome_id", (state, level, pos) -> {
       if( level instanceof RenderChunkRegion region) {
           Holder<Biome> biome = region.level.getBiome(pos);
           return region.level.registryAccess().registry(Registries.BIOME).get().getId(biome.value()) / 256f;
       }return 0;
    });
}