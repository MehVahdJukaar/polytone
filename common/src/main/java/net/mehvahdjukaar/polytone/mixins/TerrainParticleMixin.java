package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin {

    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockColors;getTintSource(Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/client/color/block/BlockTintSource;")
    )
    public @Nullable BlockTintSource polytone$whyIsGrassBlockHardcoded(@Nullable BlockTintSource original, @Local(argsOnly = true) BlockState state) {
        Boolean b = Polytone.BLOCK_MODIFIERS.getTerrainTintOverride(state.getBlock());
        if (b != null && !b) return null;
        return original;
    }
}
