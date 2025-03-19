package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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

@Pseudo
@Mixin(BlockRenderer.class)
public class SodiumBlockRendererMixin {

    @ModifyExpressionValue(method = "renderModel",
            remap = false,
            at = @At(value = "INVOKE",
                    remap = true,
                    target = "Lnet/minecraft/world/level/block/state/BlockState;hasOffsetFunction()Z"))
    private boolean polytone$modifyVisualOffset(boolean original, @Local(argsOnly = true) BlockRenderContext context) {
        return original || Polytone.BLOCK_MODIFIERS.hasVisualOffset(context.state());
    }

    @WrapOperation(method = "renderModel",
            remap = false,
            at = @At(value = "INVOKE",
                    remap = true,
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))

    private Vec3 polytone$modifyVisualOffset(BlockState state, BlockGetter blockGetter, BlockPos pos, Operation<Vec3> operation) {
        Vec3 off = Polytone.BLOCK_MODIFIERS.maybeModifyOffset(state, blockGetter, pos);
        if (off != null) return off;
        return operation.call(state, blockGetter, pos);
    }
}
