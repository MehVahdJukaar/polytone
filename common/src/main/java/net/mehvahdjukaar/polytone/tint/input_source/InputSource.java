package net.mehvahdjukaar.polytone.tint.input_source;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;


public interface InputSource {

    Codec<? extends InputSource> getCodec();

    float getValue(BlockState state, BlockAndTintGetter level, BlockPos pos);


}
