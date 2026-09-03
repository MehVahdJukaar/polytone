package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pack.class)
public class PackMixin {

    @Inject(method = "readPackMetadata", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/PackResources;getMetadataSection(Lnet/minecraft/server/packs/metadata/MetadataSectionSerializer;)Ljava/lang/Object;",
            ordinal = 0, shift = At.Shift.BEFORE))
    private static void polytone$onReadPackMetadata(PackLocationInfo location, Pack.ResourcesSupplier resources, int version,
                                                    CallbackInfoReturnable<Pack.Metadata> cir,
                                                    @Local PackResources packResources) {
        Polytone.CONFIGS.loadCurrentPackConfigs(packResources, resources, location, version);
        PackInfos.readFrom(packResources);
    }

    @Inject(method = "readPackMetadata", at = @At("RETURN"))
    private static void polytone$afterReadPackMetadata(PackLocationInfo location, Pack.ResourcesSupplier resources, int version,
                                                       CallbackInfoReturnable<Pack.Metadata> cir) {
        Polytone.CONFIGS.clearCurrentPackConfigs();
    }
}
