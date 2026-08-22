package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pack.class)
public class PackMixin {

    @Inject(method = "readPackMetadata", at =
    @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/metadata/pack/PackMetadataSection;forPackType(Lnet/minecraft/server/packs/PackType;)Lnet/minecraft/server/packs/metadata/MetadataSectionType;",
            shift = At.Shift.BEFORE))
    private static void polytone$onReadPackMetadata(PackLocationInfo packLocationInfo,
                                                    Pack.ResourcesSupplier resourcesSupplier,
                                                    PackFormat packFormat, PackType packType,
                                                    CallbackInfoReturnable<Pack.Metadata> cir,
                                                    @Local PackResources packResources) {
        Polytone.CONFIGS.loadCurrentPackConfigs(packResources, resourcesSupplier, packLocationInfo, packFormat, packType);
        PackInfos.readFrom(packResources, packType);
    }

    // The per-pack registry is only meant to be visible while this pack's own overlay conditions are being
    // evaluated. Leaving it set would make that thread (pack discovery runs on the render thread) keep
    // resolving config() against the last scanned pack forever.
    @Inject(method = "readPackMetadata", at = @At("RETURN"))
    private static void polytone$afterReadPackMetadata(CallbackInfoReturnable<Pack.Metadata> cir) {
        Polytone.CONFIGS.clearCurrentPackConfigs();
    }
}
