package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IColormapModExp {

    Codec<IColormapModExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    CodecUtils.LENIENT_FLOAT.xmap(
                            aDouble -> (IColormapModExp) (a, b, c, d, e, f, g, h, i) -> aDouble,
                            i -> 0.0f
                    ),
                    ColormapModExp.TYPE.codec()));

    float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level,
                   @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                   @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack);

    default IColormapModExp createConcurrent() {
        return this;
    }
}
