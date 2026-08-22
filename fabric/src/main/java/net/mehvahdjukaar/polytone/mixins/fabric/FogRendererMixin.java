package net.mehvahdjukaar.polytone.mixins.fabric;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void polytone$fluidFogColormap(Camera camera, float partialTicks, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector4f dest, CallbackInfo ci) {
        if (!Polytone.FLUID_MODIFIERS.hasAnyModifier()) return;
        BlockPos pos = camera.blockPosition();
        FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty()) return;
        IColorGetter fog = Polytone.FLUID_MODIFIERS.getFogColormap(fluid.getType());
        if (fog == null) return;
        if (camera.position().y >= pos.getY() + fluid.getHeight(level, pos)) return;
        float[] rgb = ColorUtils.unpack(fog.colorInWorld(level.getBlockState(pos), level, pos));
        dest.set(rgb[0], rgb[1], rgb[2], dest.w);
    }
}
