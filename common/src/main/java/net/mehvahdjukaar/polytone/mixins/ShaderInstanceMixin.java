package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.PolyGlobalUniforms;
import net.mehvahdjukaar.polytone.content.shaders.ShaderUniformsManager;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Unique
    private Boolean polytone$declaresGlobals = null;
    @Unique
    private List<ResourceLocation> polytone$targetIds = List.of();

    @Inject(method = "apply", at = @At("HEAD"))
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (polytone$declaresGlobals == null) {
            polytone$declaresGlobals = PolyGlobalUniforms.isAnyDeclaredBy(shader);
            polytone$targetIds = ShaderUniformsManager.targetIdsOf(shader);
        }
        if (polytone$declaresGlobals) Polytone.POST_SHADERS.globals().applyTo(shader::safeGetUniform);
        Polytone.SHADER_EFFECTS.applyTo(shader, polytone$targetIds);
    }
}
