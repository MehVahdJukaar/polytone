package net.mehvahdjukaar.polytone.mixins;

import it.unimi.dsi.fastutil.ints.IntList;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.FireworkParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FireworkParticles.Starter.class)
public class FireworkStarterMixin {

    //unfortunatley firework colors are chosen and mixed server side...
    @ModifyVariable(method = "tick()V", at = @At("STORE"), ordinal = 0)
    private IntList polytone$remapColors(IntList colors) {
        return Polytone.COLORS.remapFireworkColors(colors);
    }
    @ModifyVariable(method = "tick()V", at = @At("STORE"), ordinal = 1)
    private IntList polytone$remapFadeColors(IntList fadeColors) {
        return Polytone.COLORS.remapFireworkColors(fadeColors);
    }
}
