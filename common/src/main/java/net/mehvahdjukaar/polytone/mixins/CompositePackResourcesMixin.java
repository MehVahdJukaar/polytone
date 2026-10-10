package net.mehvahdjukaar.polytone.mixins;

import com.google.common.base.Suppliers;
import net.mehvahdjukaar.polytone.common.OverlayIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(CompositePackResources.class)
public class CompositePackResourcesMixin {

    @Shadow
    @Final
    private List<PackResources> packResourcesStack;

    // vanilla asks every layer's disk on each lookup, so a miss costs one disk call per overlay
    @Unique
    private final Supplier<OverlayIndex> polytone$index = Suppliers.memoize(() -> OverlayIndex.of(this.packResourcesStack));

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void polytone$getIndexedResource(PackType type, Identifier location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        OverlayIndex index = this.polytone$index.get();
        if (index != null) cir.setReturnValue(index.getResource(type, location));
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void polytone$listIndexedResources(PackType type, String namespace, String directory,
                                               PackResources.ResourceOutput output, CallbackInfo ci) {
        OverlayIndex index = this.polytone$index.get();
        if (index != null) {
            index.listResources(type, namespace, directory, output);
            ci.cancel();
        }
    }

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void polytone$getIndexedNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        OverlayIndex index = this.polytone$index.get();
        if (index != null) cir.setReturnValue(index.getNamespaces(type));
    }
}
