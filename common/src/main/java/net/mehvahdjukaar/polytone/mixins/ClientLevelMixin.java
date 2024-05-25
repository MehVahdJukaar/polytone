package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    public void polytone$extraParticles(Block instance, BlockState state, Level level, BlockPos pos, RandomSource random,
                                        Operation<Void> original) {
        original.call(instance, state, level, pos, random);

        Polytone.BLOCK_PROPERTIES.maybeEmitParticle(instance, state, level, pos);
    }

    @WrapOperation(method = "getSkyColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 polytone$modifySkyColor(Vec3 center, CubicSampler.Vec3Fetcher fetcher,
                                         Operation<Vec3> original) {
        Vec3 modified = Polytone.DIMENSION_EFFECTS.modifySkyColor(center, (ClientLevel) (Object) this);
        if (modified != null) return modified;
        return original.call(center, fetcher);
    }
}
