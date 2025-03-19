package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class SodiumBlockRendererMixin  {

    @ModifyExpressionValue(method = "renderModel",
            remap = false,
            at = @At(value = "INVOKE",
                    remap = true,
                    target = "Lnet/minecraft/world/level/block/state/BlockState;hasOffsetFunction()Z"))
    private boolean polytone$modifyVisualOffset(boolean original, @Local(argsOnly = true) BlockState state, @Local(ordinal = 0, argsOnly = true) BlockPos pos) {
        return original || Polytone.BLOCK_MODIFIERS.hasVisualOffset(state);
    }

    @WrapOperation(method = "renderModel",
            remap = false,
            at = @At(value = "INVOKE",
                    remap = true,
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))

    private Vec3 polytone$modifyVisualOffset(BlockState state, BlockPos pos, Operation<Vec3> operation) {
        var off = Polytone.BLOCK_MODIFIERS.maybeModifyOffset(state, pos);
        if(off != null) return off;
        return operation.call(state, pos);
    }
}
