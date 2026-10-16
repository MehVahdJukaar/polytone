package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {

    @ModifyArg(method = "loadShader(Lnet/minecraft/resources/Identifier;Lnet/minecraft/server/packs/resources/Resource;Lcom/mojang/blaze3d/shaders/ShaderType;Ljava/util/Map;Lcom/google/common/collect/ImmutableMap$Builder;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"))
    private static String polytone$patchLightmapLookups(String source, @Local(argsOnly = true) Identifier location,
                                                        @Local(argsOnly = true) ShaderType type,
                                                        @Local(argsOnly = true) Map<Identifier, Resource> files) {
        if (type != ShaderType.VERTEX) return source;
        boolean builtInLights = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight();
        boolean canPatch = builtInLights && files.containsKey(LightShaderPatcher.INCLUDE_LOCATION);
        Identifier shaderId = type.idConverter().fileToId(location);
        //sodium terrain pipelines load their glsl through here too
        if (shaderId.getNamespace().equals("sodium")) {
            return canPatch ? LightShaderPatcher.patchSodiumChunkShader(source) : source;
        }
        //our includ pulls in Globals, which clashes with mods that declare its members in their own blocks with same name!!
        boolean areWeSafeToAddGlobals = shaderId.getNamespace().equals("minecraft") ||
                LightShaderPatcher.importsMojGlobals(source);
        return LightShaderPatcher.onLoadVertexShader(shaderId, source, canPatch && areWeSafeToAddGlobals);
    }
}
