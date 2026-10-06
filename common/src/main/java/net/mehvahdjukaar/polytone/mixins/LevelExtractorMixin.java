package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    //every section rebuild ends up in this private overload
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void poly$invalidateParticleLight(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
        ParticleLightCache.markSectionDirty(x, y, z);
    }
}
