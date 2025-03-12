package net.mehvahdjukaar.polytone.mixins.fabric;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogFluidRendererMixin {
    @Inject(method = "computeFogColor", at = @At(value = "TAIL"), cancellable = true)
    private static void polytone$modifyFluidFogColor(Camera camera, float f, ClientLevel level, int i, float g, CallbackInfoReturnable<Vector4f> cir) {
        // Modify fog color depending on the fluid
        Vector4f output = cir.getReturnValue();
        BlockPos pos = camera.getBlockPosition();
        FluidState state = level.getFluidState(pos);
        if (camera.getPosition().y < (double) ((float) pos.getY() +
                state.getHeight(level, pos))) {
            FluidPropertyModifier modifier = Polytone.FLUID_MODIFIERS.getModifier(state.getType());
            if (modifier != null) {
                BlockColor col = modifier.getFogColormap();
                if (col != null) {
                    var newC = ColorUtils.unpack(col.getColor(null, level, pos, -1) | 0xff000000);
                    output.set(newC[0], newC[1], newC[2], output.w);
                    cir.setReturnValue(output);
                }
            }
        }
    }

}
