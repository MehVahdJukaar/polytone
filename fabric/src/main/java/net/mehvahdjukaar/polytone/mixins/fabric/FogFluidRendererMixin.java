package net.mehvahdjukaar.polytone.mixins.fabric;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogFluidRendererMixin {

    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor", at = @At(value = "TAIL"))
    private static void polytone$modifyFluidFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        // Modify fog color depending on the fluid
        FluidState state = level.getFluidState(camera.getBlockPosition());
        if (camera.getPosition().y < (double) ((float) camera.getBlockPosition().getY() +
                state.getHeight(level, camera.getBlockPosition()))) {
            var modifier = Polytone.FLUID_MODIFIERS.getModifier(state.getType());
            if (modifier != null) {
                var col = modifier.getFogColormap();
                if (col != null) {
                    var newC = ColorUtils.unpack(col.getColor(null, level, null, -1) | 0xff000000);
                    fogRed = newC[0];
                    fogGreen = newC[1];
                    fogBlue = newC[2];
                    RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
                }
            }
        }
    }

}
