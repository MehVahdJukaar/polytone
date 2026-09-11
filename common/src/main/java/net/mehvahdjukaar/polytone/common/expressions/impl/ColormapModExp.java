package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockTintProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ColormapModExp extends PolyExp implements IColormapModExp {

    public static final PolyExpType<ColormapModExp> TYPE = new PolyExpType<>(ColormapModExp::new,
            c -> c.input(BlockTintProxy.class, "o", "object").input(RandomProxy.class, "r", "random"));

    public ColormapModExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        RandomProxy rand = pos == null ? RandomProxy.GLOBAL : RandomProxy.posSeeded(BlockPos.containing(pos));
        return (float) executeDouble(new BlockTintProxy(level, pos, state, biome, r, g, b), rand);
    }

}
