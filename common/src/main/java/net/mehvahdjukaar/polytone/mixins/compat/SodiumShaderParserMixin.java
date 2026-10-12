package net.mehvahdjukaar.polytone.mixins.compat;

import net.caffeinemc.mods.sodium.client.gl.shader.ShaderParser;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(ShaderParser.class)
public abstract class SodiumShaderParserMixin {

    //darn veil... not in shaderloader as veil completely skips that
    @ModifyVariable(method = "parseShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderParser$ParsedShader;",
            remap = false, at = @At("HEAD"), argsOnly = true)
    private static String polytone$patchChunkShader(String source) {
        boolean builtInLights = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight();
        boolean canPatch = builtInLights && LightShaderPatcher.isIncludeAvailable();
        return canPatch ? LightShaderPatcher.patchSodiumChunkShader(source) : source;
    }
}
