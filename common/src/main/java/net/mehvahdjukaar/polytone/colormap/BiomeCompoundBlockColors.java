package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.utils.BiomeKeysCache;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

// basically a map of colormap to tint color
public class BiomeCompoundBlockColors implements BlockColor, ColorResolver {

    final Map<ResourceKey<Biome>, BlockColor> getters = new Object2ObjectOpenHashMap<>();
    final BlockColor defaultGetter;

    protected static ResourceKey<Biome> DEFAULT_KEY = ResourceKey.create(Registries.BIOME, new ResourceLocation("default"));
    protected static final Codec<BiomeCompoundBlockColors> DIRECT_CODEC = validate(
            Codec.unboundedMap(ResourceLocation.CODEC.xmap(r -> ResourceKey.create(Registries.BIOME, r), ResourceKey::location), Colormap.CODEC)
                    .xmap(BiomeCompoundBlockColors::new, comp -> comp.getters),
            c -> {
                if (c.getters.size() == 0) {
                    return DataResult.error(() -> "Compound Biome Colormap Must have at least 2 tint getter");
                }
                if (!c.getters.containsKey(DEFAULT_KEY)) {
                    return DataResult.error(() -> "Compound Biome Colormap MUST have a tint getter with the key 'default'");
                }
                return DataResult.success(c);
            });

    public static <T> Codec<T> validate(Codec<T> codec, Function<T, DataResult<T>> function) {
        return codec.flatXmap(function, function);
    }


    private BiomeCompoundBlockColors(Map<ResourceKey<Biome>, BlockColor> map) {
        this.getters.putAll(map);
        this.defaultGetter = getters.get(DEFAULT_KEY);
    }

    @Override
    public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (pos == null) {
            return defaultGetter.getColor(state, level, pos, tintIndex);
        }
        return level.getBlockTint(pos, this);
    }


    @Override
    public int getColor(Biome biome, double d, double e) {
        ResourceKey<Biome> key = BiomeKeysCache.get(biome);
        return ((Colormap) getters.getOrDefault(key, defaultGetter))
                .getColor(biome, d, e);
    }
}
