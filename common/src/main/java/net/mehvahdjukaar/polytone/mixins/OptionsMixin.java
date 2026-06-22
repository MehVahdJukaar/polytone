package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
public class OptionsMixin {

    // Force a resource pack reload when configs changed so overlay conditions re-resolve
    @ModifyExpressionValue(method = "updateResourcePacks",
            at = @At(value = "INVOKE", target = "Ljava/util/List;equals(Ljava/lang/Object;)Z"))
    public boolean polytone$reloadWhenConfigsChanged(boolean original) {
        if (Polytone.CONFIGS.checkAndClearNeedsPackReload()) {
            return false;
        }
        return original;
    }
}
