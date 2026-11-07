package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.shaders.Uniform;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
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
    private List<IShaderInputPatch> polytone$inputPatches = null;
    @Unique
    private IShaderInputs polytone$inputs = null;
    @Unique
    private int polytone$inputPatchesProgramId = -1;

    @Inject(method = "apply", at = @At("HEAD"))
    private void polytone$bindExtraUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (polytone$inputPatches == null || polytone$inputPatchesProgramId != shader.getId()) {
            polytone$inputPatchesProgramId = shader.getId();
            polytone$inputs = IShaderInputs.ofShaderInstance(shader, uniforms);
            polytone$inputPatches = PolytoneRenderHooks.inputPatchesFor(polytone$inputs);
        }
        for (var patch : polytone$inputPatches) {
            patch.bindToShader(polytone$inputs);
        }
    }
}
