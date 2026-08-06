package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    // Every section rebuild (block or light change) funnels through this private overload - the public
    // 3-arg one and setSectionRangeDirty/WithNeighbors all delegate here. Bump that section's
    // light-cache version so particles inside it re-sample. Section coords come in directly.
    // 26.2: this used to live on LevelRenderer.
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void poly$invalidateParticleLight(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
        ParticleLightCache.markSectionDirty(x, y, z);
    }
}
