package net.mehvahdjukaar.polytone.mixins.fabric;

//@Mixin(FogRenderer.class)
public abstract class FogRendererMixin2 {

    /*
    @Inject(method = "setupFog", at = @At(value = "TAIL"), cancellable = true)
    private static void polytone$modifyFogShape(Camera camera, FogRenderer.FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> cir, @Local FogType fogType) {
        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && fogType == FogType.NONE) {
            var newFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(
                    cir.getReturnValue().start(), cir.getReturnValue().end());
            if (newFog != null) {
                FogParameters old = cir.getReturnValue();
                cir.setReturnValue(new FogParameters(newFog.x, newFog.y, old.shape(), old.red(), old.green(), old.blue(), old.alpha()));
            }

        }
    }*/

}
