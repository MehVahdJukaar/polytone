package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pack.class)
public class PackMixin {

    // Load this pack's configs eagerly, before the overlay metadata section (and its conditions) are parsed
    @Inject(method = "readPackMetadata", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/PackResources;getMetadataSection(Lnet/minecraft/server/packs/metadata/MetadataSectionSerializer;)Ljava/lang/Object;",
            ordinal = 0, shift = At.Shift.BEFORE))
    private static void polytone$onReadPackMetadata(PackLocationInfo location, Pack.ResourcesSupplier resources, int version,
                                                    CallbackInfoReturnable<Pack.Metadata> cir,
                                                    @Local PackResources packResources) {
        Polytone.CONFIGS.loadCurrentPackConfigs(packResources);
    }
}
