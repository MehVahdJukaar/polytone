package net.mehvahdjukaar.polytone.mixins.accessor;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(SodiumWorldRenderer.class)
public interface SodiumWorldRendererShadowAccessor {

    @Accessor(value = "renderSectionManager", remap = false)
    RenderSectionManager polytone$getRenderSectionManager();

    @Accessor(value = "uniformBufferManager", remap = false)
    UniformBufferManager polytone$getUniformBufferManager();
}