package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {


    @WrapOperation(method = "<init>", at = @At(value = "NEW",
            target = "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation moonlight$namespacedShader(String location,
                                                        Operation<ResourceLocation> original,
                                                        @Local(argsOnly = true) String name) {
        if (name.contains("polytone_marker")) {
            var res = new ResourceLocation(name.replace("polytone_marker", ":"));
            String namespace = res.getNamespace();
            String path = res.getPath();
            return new ResourceLocation(namespace, "shaders/core/" + path + ".json");

        }
        return original.call(location);
    }
}