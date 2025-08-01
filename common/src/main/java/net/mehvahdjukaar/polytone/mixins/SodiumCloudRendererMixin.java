package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(CloudRenderer.class)
public class SodiumCloudRendererMixin {

    @ModifyExpressionValue(method = "render",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private float polytone$whyDoesSodiumHaveToReplaceEntireClasses(float original, @Local(argsOnly = true) ClientLevel level) {
        Float f = Polytone.DIMENSION_MODIFIERS.modifyCloudHeight(level);
        return f != null ? f : original;
    }
}
