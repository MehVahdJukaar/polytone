package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.CodecUtil;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BiomeCompoundColorGetter implements IColorGetter {

    public static final Codec<BiomeCompoundColorGetter> CODEC =
            CodecUtil.validate(
            RecordCodecBuilder.<BiomeCompoundColorGetter>create(i -> i.group(
                    Colormap.REFERENCE_OR_EXPRESSION.fieldOf("default")
                            .forGetter(c -> c.defaultGetter),
                    CodecUtils.lenientUnboundedMap(
                                    RegistryFixedCodec.create(Registries.BIOME),
                                    Colormap.REFERENCE_OR_EXPRESSION
                            )
                            .fieldOf("biomes").forGetter(c -> c.holderMap)
            ).apply(i, BiomeCompoundColorGetter::new)),
                    c -> {
                        if (c.getters.isEmpty()) {
                            return DataResult.error(() -> "Must have at least 1 tint getter");
                        } else {
                            for (var a : c.getters.values()) {
                                if (a.needsToFillTexture()) {
                                    return DataResult.error(() -> "Biome compound colormap only works on BY_REFERENCE colormaps defined in the colormap folder");
                                }
                            }
                            if (c.defaultGetter.needsToFillTexture()) {
                                return DataResult.error(() -> "Biome compound colormap only works on BY_REFERENCE colormaps defined in the colormap folder");
                            }
                        }
                        return DataResult.success(c);
                    });

    private final Map<Biome, IColorGetter> getters = new HashMap<>();
    private final Map<Holder<Biome>, IColorGetter> holderMap;
    private final IColorGetter defaultGetter;

    public BiomeCompoundColorGetter(IColorGetter defaultGetter, Map<Holder<Biome>, IColorGetter> map) {
        for (var e : map.entrySet()) {
            this.getters.put(e.getKey().value(), e.getValue());
        }
        this.holderMap = map;
        this.defaultGetter = defaultGetter;
    }

    @Override
    public boolean needsToFillTexture() {
        return false;
    }

    @Override
    public int getColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int i) {
        if (level instanceof LevelReader l) {
            Biome biome = l.getBiome(pos).value();
            IColorGetter g = getters.get(biome);
            if (g != null) return g.getColor(blockState, level, pos, i);
        }
        return defaultGetter.getColor(blockState, level, pos, i);
    }

    @Override
    public int getColor(ItemStack itemStack, int i) {
        return defaultGetter.getColor(itemStack, i);
    }

    @Override
    public IColorGetter makeConcurrent() {
        Map<Holder<Biome>, IColorGetter> map = new HashMap<>();
        for (var e : holderMap.entrySet()) {
            map.put(e.getKey(), e.getValue().makeConcurrent());
        }
        return new BiomeCompoundColorGetter(defaultGetter.makeConcurrent(), map);
    }

    @Override
    public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
        if (biome != null) {
            IColorGetter g = getters.get(biome);
            if (g != null) return g.sampleColor(state, pos, biome, item);
        }
        return defaultGetter.sampleColor(state, pos, biome, item);
    }
}