package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// ports neoforge's vanilla patch to fabric that lets EffectInstance resolve namespaced shader program names
@Mixin(EffectInstance.class)
public abstract class EffectInstanceMixin {

    @WrapOperation(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation polytone$resolveNamespacedJson(String fullPath, Operation<ResourceLocation> original) {
        return polytone$tryRebuild(fullPath, original);
    }

    @WrapOperation(method = "getOrCreate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation polytone$resolveNamespacedProgram(String fullPath, Operation<ResourceLocation> original) {
        return polytone$tryRebuild(fullPath, original);
    }

    @Unique
    private static ResourceLocation polytone$tryRebuild(String fullPath, Operation<ResourceLocation> original) {
        if (!Polytone.POST_CHAINS.isLoadingChain()) {
            return original.call(fullPath);
        }
        final String prefix = "shaders/program/";
        if (fullPath != null && fullPath.startsWith(prefix)) {
            int colon = fullPath.indexOf(':', prefix.length());
            if (colon > 0) {
                try {
                    String namespace = fullPath.substring(prefix.length(), colon);
                    String path = prefix + fullPath.substring(colon + 1);
                    ResourceLocation rl = ResourceLocation.tryBuild(namespace, path);
                    if (rl != null) return rl;
                } catch (Exception ignored) {
                }
            }
        }
        return original.call(fullPath);
    }
}
