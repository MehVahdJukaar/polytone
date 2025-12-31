package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public class EnvironmentSystemMixin {

    @Inject(method = "addDefaultLayers", at = @At("RETURN"))
    private static void polytone$addCustomPostLayers(EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        Polytone.DIMENSION_MODIFIERS.addPostLayers(builder, level);
    }
}
