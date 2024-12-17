package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {


    @Inject(method = "putQuadData", at = @At(value = "HEAD"))
    private void polytone$modifyBiomeTexture(BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer consumer,
                                             PoseStack.Pose pose, BakedQuad quad,
                                             float brightness0, float brightness1, float brightness2, float brightness3,
                                             int lightmap0, int lightmap1, int lightmap2, int lightmap3,
                                             int packedOverlay, CallbackInfo ci,
                                             @Local(argsOnly = true) LocalRef<BakedQuad> mutableQuad) {
        BakedQuad newQuad = Polytone.VARIANT_TEXTURES.maybeModify(quad, level, state, pos);

        if (newQuad != null) {
            mutableQuad.set(newQuad);
        }
    }

    @WrapOperation(method = "tesselateBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 polytone$modifyVisualOffset(BlockState state, BlockGetter blockGetter, BlockPos pos, Operation<Vec3> original) {
        var m = Polytone.BLOCK_MODIFIERS.maybeModify(state, blockGetter, pos);
        if (m != null) {
            return m;
        }
        return original.call(state, blockGetter, pos);
    }
}
