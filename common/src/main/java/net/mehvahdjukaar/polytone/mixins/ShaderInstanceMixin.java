package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.shaders.Uniform;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
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
    private List<IShaderInputPatch> polytone$declaredBinders = null;
    @Unique
    private IShaderInputs polytone$instance = null;

    @Inject(method = "apply", at = @At("HEAD"))
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (polytone$declaredBinders == null) {
            polytone$instance = IShaderInputs.ofShaderInstance(shader, uniforms);
            polytone$declaredBinders = IShaderInputPatch.allApplyingTo(polytone$instance);
        }
        for (var binder : polytone$declaredBinders) {
            binder.bindToShader(polytone$instance);
        }
    }
}
