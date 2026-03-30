package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlProgram;
import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;

@Mixin(GlProgram.class)
public class GlProgramMixin {

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet([Ljava/lang/Object;)Ljava/util/HashSet;"))
    private static HashSet<String> method_67884(HashSet<String> original) {
        HashSet<String> s = new HashSet<>();
        s.add(PostShadersManager.GLOBALS_NAME);
        s.addAll(original);
        return s;
    }
}
