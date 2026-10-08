package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Unique
    private List<IShaderModifier> polytone$declaredModifiers = null;
    @Unique
    private IShader polytone$instance = null;

    //vanilla owns these draws, so apply is the only spot. HEAD so the values upload in this same apply
    @Inject(method = "apply", at = @At("HEAD"))
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (polytone$declaredModifiers == null) {
            polytone$instance = IShader.ofShaderInstance(shader);
            polytone$declaredModifiers = IShaderModifier.usedBy(polytone$instance);
        }
        for (var modifier : polytone$declaredModifiers) {
            modifier.bindTo(polytone$instance);
        }
    }
}
