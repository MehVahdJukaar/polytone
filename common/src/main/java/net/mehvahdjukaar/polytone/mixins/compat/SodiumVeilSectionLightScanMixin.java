package net.mehvahdjukaar.polytone.mixins.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.light.BlockLightSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class SodiumVeilSectionLightScanMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private void polytone$openLightScan(CallbackInfoReturnable<ChunkBuildOutput> cir,
                                        @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        scan.set(Polytone.COLORED_LIGHTS.openBlockScan());
    }

    @WrapOperation(method = "execute", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState polytone$scanForLights(LevelSlice slice, int x, int y, int z, Operation<BlockState> original,
                                              @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        BlockState state = original.call(slice, x, y, z);
        var s = scan.get();
        if (s != null) s.offer(x, y, z, state);
        return state;
    }

    @Inject(method = "execute", at = @At("RETURN"), remap = false)
    private void polytone$publishLightScan(CallbackInfoReturnable<ChunkBuildOutput> cir,
                                           @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        var s = scan.get();
        if (s != null) s.publish();
    }
}
