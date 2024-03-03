package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractBlockRenderContext.class)
public class ModelHackMixin {


    @Shadow
    @Final
    protected BlockRenderInfo blockInfo;

    @ModifyArg(method = "renderQuad",
            remap = false,
            at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext;colorizeQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;I)V"))
    public int modifyModels(int original) {
        if(original == -1 && Polytone.VARIANT_TEXTURES.shouldSetTintTo0(original, blockInfo.blockView, blockInfo.blockState, blockInfo.blockPos)){
            return 0;
        }
        return original;
    }

}
