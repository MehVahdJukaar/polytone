package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.shaders.Uniform;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Unique
    private List<IShaderModifier> polytone$declaredModifiers = null;
    @Unique
    private IShader polytone$instance = null;

    @Inject(method = "apply", at = @At("HEAD"))
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (polytone$declaredModifiers == null) {
            polytone$instance = IShader.ofShaderInstance(shader, uniforms);
            polytone$declaredModifiers = IShaderModifier.usedBy(polytone$instance);
        }
        for (var modifier : polytone$declaredModifiers) {
            modifier.bindTo(polytone$instance);
        }
    }
}
