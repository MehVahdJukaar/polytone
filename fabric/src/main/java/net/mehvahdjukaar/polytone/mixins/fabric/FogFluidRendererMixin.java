package net.mehvahdjukaar.polytone.mixins.fabric;

import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FogEnvironment.class)
public abstract class FogFluidRendererMixin {
    //TODO: add back
/*
    @Inject(method = "computeFogColor", at = @At(value = "TAIL"), cancellable = true)
    private static void polytone$modifyFluidFogColor(Camera camera, float f, ClientLevel clientLevel, int i, float g, boolean bl, CallbackInfoReturnable<Vector4f> cir) {
        // Modify fog color depending on the fluid
        Vector4f output = cir.getReturnValue();
        BlockPos pos = camera.getBlockPosition();
        FluidState state = clientLevel.getFluidState(pos);
        if (camera.getPosition().y < (double) ((float) pos.getY() +
                state.getHeight(clientLevel, pos))) {
            FluidPropertyModifier modifier = Polytone.FLUID_MODIFIERS.getModifier(state.getType());
            if (modifier != null) {
                BlockColor col = modifier.getFogColormap();
                if (col != null) {
                    var newC = ColorUtils.unpack(col.getColor(null, clientLevel, pos, -1) | 0xff000000);
                    output.set(newC[0], newC[1], newC[2], output.w);
                    cir.setReturnValue(output);
                }
            }
        }
    }*/
}
