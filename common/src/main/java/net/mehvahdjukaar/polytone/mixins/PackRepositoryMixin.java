package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Inject(method = "reload",
            at = @At(value = "HEAD"))
    private void polytone$beforeReload(CallbackInfo ci) {
        Polytone.CONFIGS.beforeRepositoryRefresh();
    }
}
