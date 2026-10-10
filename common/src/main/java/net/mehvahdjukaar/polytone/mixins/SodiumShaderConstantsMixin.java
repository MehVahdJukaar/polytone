package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

// every sodium terrain pass compiles from the same source, so packs can't tell them apart otherwise
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public class SodiumShaderConstantsMixin {

    @ModifyReturnValue(method = "createShaderConstants", at = @At("RETURN"), remap = false, require = 0)
    private static List<String> polytone$addRenderPassDefines(List<String> original, TerrainRenderPass pass) {
        String passDefine = polytone$passDefine(pass);
        // a pass another mod registered
        if (passDefine == null) return original;

        List<String> defines = new ArrayList<>(original);
        defines.add(passDefine);
        return defines;
    }

    @Unique
    private static String polytone$passDefine(TerrainRenderPass pass) {
        if (pass == DefaultTerrainRenderPasses.SOLID) return "RENDER_PASS_SOLID";
        if (pass == DefaultTerrainRenderPasses.CUTOUT) return "RENDER_PASS_CUTOUT";
        if (pass == DefaultTerrainRenderPasses.TRANSLUCENT) return "RENDER_PASS_TRANSLUCENT";
        return null;
    }
}
