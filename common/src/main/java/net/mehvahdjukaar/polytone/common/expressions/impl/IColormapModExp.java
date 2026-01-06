package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.ColormapModContextExpression;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.colormap.ColormapColorModulator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IColormapModExp {

    Codec<IColormapModExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.withAlternative(ColormapModContextExpression.CODEC, ColormapModExp.TYPE.codec()));

    float evaluate(float r, float g, float b,@Nullable BlockAndTintGetter level,
                          @Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                          @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack);

    IColormapModExp createConcurrent();
}
