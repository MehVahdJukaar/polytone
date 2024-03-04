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
        if (original == -1 && Polytone.VARIANT_TEXTURES.shouldSetTintTo0(original, blockInfo.blockView, blockInfo.blockState, blockInfo.blockPos)) {
            return 0;
        }
        return original;
    }

}

/*
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext.BakedModelConsumerImpl")
public class ModelHackMixin {


    @Shadow
    @Final
    AbstractBlockRenderContext this$0;

    @Inject(method = "accept(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;)V",
            remap = false,
            at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;fromVanilla(Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;Lnet/minecraft/core/Direction;)Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;",
                    shift = At.Shift.BEFORE))
    public void modifyModels(BakedModel model, BlockState state, CallbackInfo ci, @Local BakedQuad quad) {
        var blockInfo = ((BlockInfoAccessor) this$0).getBlockInfo();
        Polytone.VARIANT_TEXTURES.maybeModify(quad, blockInfo.blockView, blockInfo.blockState, blockInfo.blockPos);
    }

}*/
