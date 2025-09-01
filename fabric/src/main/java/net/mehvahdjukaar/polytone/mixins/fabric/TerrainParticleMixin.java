package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin {


    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    public boolean polytone$whyIsGrassBlockHardcoded(boolean original, @Local(argsOnly = true) BlockState state) {
        Boolean b = Polytone.BLOCK_MODIFIERS.getTerrainTintOverride(state.getBlock());
        if (b != null) return b;
        return original;
    }
}
