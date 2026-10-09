package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeSystem.Builder.class)
public class EnvironmentSystemMixin {

    @Inject(method = "addDefaultLayers", at = @At("RETURN"))
    private void polytone$addCustomPostLayers(Level level, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
        EnvironmentAttributeSystem.Builder builder = (EnvironmentAttributeSystem.Builder) (Object) this;
        Polytone.BIOME_MODIFIERS.addPostLayers(builder, level);
        Polytone.DIMENSION_MODIFIERS.addPostLayers(builder, level);
    }
}
