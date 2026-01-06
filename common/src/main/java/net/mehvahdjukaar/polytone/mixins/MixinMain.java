package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.data.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {

    @Inject(at = @At("HEAD"), method = "main", remap = false)
    private static void m(String[] args, CallbackInfo info) {
        if (Polytone.isDevEnv) System.setProperty("joml.fastmath", "true");
    }

}