package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GlslPreprocessor.class)
public abstract class GlslPreprocessorMixin {

    //again for veil.. it recompiles every vanilla shader with its own preprocessor and never goes through Program
    @ModifyVariable(method = "process", at = @At("HEAD"), argsOnly = true)
    private String polytone$modifyShader(String source) {
        boolean builtInLights = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight();
        boolean canPatch = builtInLights && LightShaderPatcher.isIncludeAvailable();
        return canPatch ? LightShaderPatcher.patch(source) : source;
    }
}
