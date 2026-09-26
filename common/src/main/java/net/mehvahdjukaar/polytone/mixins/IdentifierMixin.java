package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.common.IdentifierSanityCheck;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Identifier.class)
public class IdentifierMixin {

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;)V", at = @At("RETURN"))
    private void poly$validateValidNamespace(String namespace, String path, CallbackInfo ci) {
        IdentifierSanityCheck.validateNamespace(namespace, path);
    }
}
