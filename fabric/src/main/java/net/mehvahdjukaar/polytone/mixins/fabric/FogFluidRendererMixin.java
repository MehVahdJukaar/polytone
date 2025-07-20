package net.mehvahdjukaar.polytone.mixins.fabric;

//@Mixin(FogRenderer.class)
public abstract class FogFluidRendererMixin {
    /*
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
    }*/

}
