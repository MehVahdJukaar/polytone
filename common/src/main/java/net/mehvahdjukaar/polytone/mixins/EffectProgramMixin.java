package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.EffectProgram;
import net.mehvahdjukaar.polytone.content.shaders.post.PostProgramImports;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EffectProgram.class)
public abstract class EffectProgramMixin {

    @ModifyArg(method = "compileShader", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/shaders/EffectProgram;compileShaderInternal(Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;Ljava/io/InputStream;Ljava/lang/String;Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;)I"), index = 4)
    private static GlslPreprocessor polytone$fixMojImports(GlslPreprocessor original) {
        return new PostProgramImports();
    }
}
