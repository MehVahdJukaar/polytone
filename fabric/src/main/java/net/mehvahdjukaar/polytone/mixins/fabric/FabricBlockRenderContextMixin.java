package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockRenderContext.class)
public class FabricBlockRenderContextMixin {

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 polytone$modifyVisualOffset(BlockState state, BlockGetter blockGetter, BlockPos pos, Operation<Vec3> original) {
        Vec3 m = Polytone.BLOCK_MODIFIERS.maybeModifyOffset(state, pos);
        if (m != null) {
            return m;
        }
        return original.call(state, blockGetter, pos);
    }
}
