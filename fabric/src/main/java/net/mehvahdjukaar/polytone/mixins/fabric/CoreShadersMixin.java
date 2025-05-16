package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.fabric.PlatStuffImpl;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(CoreShaders.class)
public abstract class CoreShadersMixin {

    @ModifyReturnValue(method = "getProgramsToPreload", at = @At(value = "RETURN"))
    private static List<ShaderProgram> polytone$registerShaders(List<ShaderProgram> original) {
        var newList = new ArrayList<>(original);
        PlatStuffImpl.SHADER_REGISTRATIONS.forEach(r -> {
            try {
                newList.add(r.create());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load shader: " + r.id(), e);
            }
        });
        PlatStuffImpl.SHADER_REGISTRATIONS.clear();

       PlatStuffImpl. addRenderParticlesType();

        return newList;
    }
}
