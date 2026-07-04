package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockTintProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ColormapModExp extends PolyExp implements IColormapModExp {

    public static final PolyExpType<ColormapModExp> TYPE =
            new PolyExpType<>(
                    ColormapModExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", BlockTintProxy.class);
                        c.addInput("object", BlockTintProxy.class);
                    }
            );


    public ColormapModExp(Serializable ser) {
        super(ser);
    }

    @Override
    public float evaluate(float r, float g, float b, @Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        BlockTintProxy obj = new BlockTintProxy(level, pos, state, biome, r, g, b);
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("o", obj);
        vars.put("object", obj);
        RandomProxy rand = pos == null ? RandomProxy.GLOBAL : RandomProxy.posSeeded(BlockPos.containing(pos));
        vars.put("random", rand);
        vars.put("r", rand);
        return (float) executeDouble(vars);
    }

}
