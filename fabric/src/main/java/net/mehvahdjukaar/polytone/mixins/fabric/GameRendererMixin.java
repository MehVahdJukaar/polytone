package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.polytone.fabric.PlatStuffImpl;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "reloadShaders", at = @At(value = "INVOKE",
            ordinal = 52,
            shift = At.Shift.AFTER,
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    public void polytone$registerShaders(ResourceProvider resourceProvider, CallbackInfo ci,
                                          @Local(ordinal = 1) List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list) {

        PlatStuffImpl.SHADER_REGISTRATIONS.forEach(r -> {
            try {
                ShaderInstance shader = new ShaderInstance(resourceProvider, r.id().toString()
                        .replace(":", "polytone_marker"), r.format());
                list.add(Pair.of(shader, r.shaderConsumer()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load shader: " + r.id(), e);
            }
        });
        PlatStuffImpl.SHADER_REGISTRATIONS.clear();

    }
}
