package net.mehvahdjukaar.polytone.mixins.neoforge.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.light.BlockLightSource;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class VeilSectionLightScanMixin {

    @Inject(method =
            "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"
            , at = @At("HEAD"))
    private void polytone$openLightScan(CallbackInfoReturnable<SectionCompiler.Results> cir,
                                        @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        scan.set(Polytone.COLORED_LIGHTS.openBlockScan());
    }

    @WrapOperation(method =
            "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"
            ,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState polytone$scanForLights(RenderChunkRegion region, BlockPos pos, Operation<BlockState> original,
                                              @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        BlockState state = original.call(region, pos);
        var s = scan.get();
        if (s != null) s.offer(pos.getX(), pos.getY(), pos.getZ(), state);
        return state;
    }

    @Inject(method =
            "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"

            , at = @At("RETURN"))
    private void polytone$publishLightScan(CallbackInfoReturnable<SectionCompiler.Results> cir,
                                           @Share("polytone$scan") LocalRef<BlockLightSource.Scan> scan) {
        var s = scan.get();
        if (s != null) s.publish();
    }
}
