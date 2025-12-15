package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    /*
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
    }*/
//TODO 1.21.5


}
