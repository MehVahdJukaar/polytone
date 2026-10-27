package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    //sky distance stuff
    @ModifyArg(method = "setupFog", index = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/FogRenderer;computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V"))
    private int polytone$skyRenderDistance(int renderDistanceChunks) {
        Integer skyRenderDistance = Polytone.COLORS.getSkyRenderDistance();
        return skyRenderDistance != null ? skyRenderDistance : renderDistanceChunks;
    }
}
