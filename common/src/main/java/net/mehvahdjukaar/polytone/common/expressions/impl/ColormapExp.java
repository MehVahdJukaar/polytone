package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class ColormapExp extends PolyExp implements IColormapExp{


    public static final PolyExpType<ColormapExp> TYPE =
            new PolyExpType<>(
                    ColormapExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                    }
            );

    public ColormapExp(Serializable ser){
        super(ser);
    }

    @Override
    public float evaluate(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {

    }
}
