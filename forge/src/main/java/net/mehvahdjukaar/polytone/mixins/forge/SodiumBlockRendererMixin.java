package net.mehvahdjukaar.polytone.mixins.forge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(BlockRenderer.class)
public class SodiumBlockRendererMixin {

    @Inject(method = "renderModel",
            remap = false,
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    remap = true,
                    target = "Ljava/util/List;clear()V"))
    private void polytone$modifyVisualOffset(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci,
                                             @Local LocalRef<Vec3> offset) {
        Vec3 m = Polytone.BLOCK_MODIFIERS.maybeModifyOffset(ctx.state(), ctx.world(), ctx.pos());
        if (m != null) {
            offset.set(m);
        }
    }
}
