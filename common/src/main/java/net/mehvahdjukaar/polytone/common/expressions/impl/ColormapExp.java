package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColormapExp extends PolyExp implements IColormapExp {

    public static final PolyExpType<ColormapExp> TYPE = new PolyExpType<>(ColormapExp::new,
            c -> c.input(BlockProxy.class, "o", "object").input(RandomProxy.class, "r", "random"));

    private final boolean hasBiome;

    public ColormapExp(ExpProgram program, String source) {
        super(program, source);
        boolean biome = false;
        for (String member : program.usedMembers()) biome |= member.toLowerCase().contains("biome");
        this.hasBiome = biome;
    }

    @Override
    public boolean usesBiome() {
        return hasBiome;
    }

    @Override
    public float evaluate(@NotNull BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        RandomProxy rand = pos == null ? RandomProxy.GLOBAL : RandomProxy.posSeeded(BlockPos.containing(pos));
        return (float) executeDouble(new BlockProxy(level, pos, state, biome), rand);
    }
}
