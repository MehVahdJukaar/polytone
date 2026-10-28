package net.mehvahdjukaar.polytone.mixins.compat;

import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(ShaderLoader.class)
public abstract class SodiumShaderLoaderMixin {

    @ModifyArg(method = "loadShader", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser;parseShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser$ParsedShader;"))
    private static String polytone$patchChunkShader(String source) {
        return PolytoneRenderHooks.patchSodiumChunkShader(source);
    }
}
