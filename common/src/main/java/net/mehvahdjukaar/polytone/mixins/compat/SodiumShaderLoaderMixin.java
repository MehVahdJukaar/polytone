package net.mehvahdjukaar.polytone.mixins.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.mehvahdjukaar.polytone.Polytone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(ShaderLoader.class)
public abstract class SodiumShaderLoaderMixin {

    @ModifyArg(method = "loadShader", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser;parseShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser$ParsedShader;"))
    private static String polytone$patchChunkShader(String source, @Local(argsOnly = true) ShaderType type) {
        if (type != ShaderType.VERTEX) return source;
        for (var patcher : Polytone.SHADER_PATCHERS) {
            source = patcher.patchSodiumChunkShaderSource(source);
        }
        return source;
    }
}
