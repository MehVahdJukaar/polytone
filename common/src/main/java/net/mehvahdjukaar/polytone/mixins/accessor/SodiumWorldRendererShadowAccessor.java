package net.mehvahdjukaar.polytone.mixins.accessor;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches Sodium's private {@code renderSectionManager} so the shadow pass can re-cull its terrain
 * render list against the light volume (see {@code SodiumShadowRenderer}). {@code @Pseudo} so this
 * simply doesn't apply when Sodium is absent.
 */
@Pseudo
@Mixin(SodiumWorldRenderer.class)
public interface SodiumWorldRendererShadowAccessor {

    @Accessor(value = "renderSectionManager", remap = false)
    RenderSectionManager polytone$getRenderSectionManager();

    @Accessor(value = "uniformBufferManager", remap = false)
    UniformBufferManager polytone$getUniformBufferManager();
}