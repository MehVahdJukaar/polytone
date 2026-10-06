package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Program;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Program.class)
public abstract class ProgramMixin {

    @ModifyArg(method = "compileShaderInternal", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"))
    private static String polytone$modifyShader(String source, @Local(argsOnly = true) Program.Type type) {
        boolean builtInLights = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight();
        boolean canPatch = builtInLights && type == Program.Type.VERTEX && LightShaderPatcher.isIncludeAvailable();
        return canPatch ? LightShaderPatcher.patch(source) : source;
    }
}
