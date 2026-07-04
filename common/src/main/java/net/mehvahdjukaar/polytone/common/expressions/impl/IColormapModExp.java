package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.ColormapModContextExpression;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IColormapModExp {

    Codec<IColormapModExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    CodecUtils.LENIENT_FLOAT.xmap(
                            aDouble -> (_, _, _, _, _, _, _, _, _)
                                    -> aDouble,
                            _ -> 0.0f
                    ),
                    ColormapModContextExpression.CODEC, ColormapModExp.TYPE.codec()));

    float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level,
                   @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome,
                   @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack);

    default IColormapModExp createConcurrent() {
        return this;
    }
}
