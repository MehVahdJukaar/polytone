package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFogEnvironment.class)
public class LavaFogEnvironmentMixin {

    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void polytone$lavaFogColormap(ClientLevel level, Camera camera, int renderDistance,
                                          float partialTicks, CallbackInfoReturnable<Integer> cir) {
        IColorGetter fog = Polytone.FLUID_MODIFIERS.getFogColormap(Fluids.LAVA);
        if (fog == null) return;
        BlockPos pos = camera.blockPosition();
        cir.setReturnValue(fog.colorInWorld(level.getBlockState(pos), level, pos));
    }
}
