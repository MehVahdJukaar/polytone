package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "addResourcePackLoadFailToast", at = @At("HEAD"))
    public void polytone$changeToast(Component message, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Component> modifiableMessage) {
        if (Polytone.iMessedUp) {
            modifiableMessage.set(Component.translatable("toast.polytone.load_fail"));
            Polytone.iMessedUp = false;
        }
    }
}
