package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PostChain.class)
public abstract class PostChainMixin {

    //kept out of customRenderTargets, the chain destroys those on close
    @ModifyReturnValue(method = "getRenderTarget", at = @At("RETURN"))
    private RenderTarget polytone$resolvePersistentPostTarget(RenderTarget original, @Nullable String target) {
        if (original != null || target == null || !Polytone.POST_CHAINS.isLoadingChain()) {
            return original;
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id == null ? null : Polytone.POST_TARGETS.getTarget(id);
    }
}
