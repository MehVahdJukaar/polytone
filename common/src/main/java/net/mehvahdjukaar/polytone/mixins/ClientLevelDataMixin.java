package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {

    @Shadow
    @Final
    private boolean isFlat;

    @ModifyReturnValue(method = "voidDarknessOnsetRange", at = @At("RETURN"))
    public float polytone$alterDarkness(float original) {
        if (!isFlat) {
            var modified = Polytone.COLORS.getVoidDarknessOffset();
            if (modified != null) return modified;
        }
        return original;
    }


    @ModifyReturnValue(method = "getHorizonHeight", at = @At("RETURN"))
    public double polytone$alterHorizon(double original) {
        if (!isFlat) {
            var modified = Polytone.COLORS.getHorizonHeight();
            if (modified != null) return modified;
        }
        return original;
    }
}
