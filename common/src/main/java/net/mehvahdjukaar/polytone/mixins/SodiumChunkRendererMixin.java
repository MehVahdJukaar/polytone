package net.mehvahdjukaar.polytone.mixins;

import com.mojang.renderpearl.api.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class SodiumChunkRendererMixin {

    @Inject(method = "begin", at = @At("TAIL"), require = 0)
    private void polytone$captureTerrainSampler(TerrainRenderPass pass, FogParameters parameters,
                                                GpuSampler terrainSampler, OitStage oitStage, CallbackInfo ci) {
        SodiumShadowRenderer.captureTerrainSampler(terrainSampler);
    }
}
