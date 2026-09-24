package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(ResourceLocation.class)
public class IdentifierMixin {

    @Unique
    private static final Pattern POLY$VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]*");

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;)V", at = @At("RETURN"))
    private void poly$validateValidNamespace(String namespace, String path, CallbackInfo ci) {
        if (!POLY$VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalStateException("Invalid Identifier namespace: " + namespace + ":" + path +
                    ". Some mod disabled vanilla id validation, check the mixins applied to Identifier. This is VERY BAD!");
        }
    }
}
