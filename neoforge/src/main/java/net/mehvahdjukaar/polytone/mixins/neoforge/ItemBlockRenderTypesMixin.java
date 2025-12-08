package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.mehvahdjukaar.polytone.neoforge.PlatStuffImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    @Inject(method = "checkClientLoading", at = @At("HEAD"), cancellable = true)
    private static void polytone$cancelCheck(CallbackInfo ci) {
        if (PlatStuffImpl.dontCheckLoading) {
            ci.cancel();
        }
    }
}
