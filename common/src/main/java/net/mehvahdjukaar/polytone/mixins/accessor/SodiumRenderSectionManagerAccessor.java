package net.mehvahdjukaar.polytone.mixins.accessor;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Rebuilding the CAMERA render list after the shadow pass has stomped it (see
 * {@code SodiumShadowRenderer}) means redoing exactly what Sodium's own {@code finalizeRenderLists}
 * does, using the occlusion trees its async culler already produced this frame. That's this private
 * method; the public entry points either wait on state we've invalidated or fall back to a
 * frustum-only list. {@code @Pseudo} so this simply doesn't apply when Sodium is absent.
 */
@Pseudo
@Mixin(RenderSectionManager.class)
public interface SodiumRenderSectionManagerAccessor {

    @Invoker(value = "readRenderListFromTree", remap = false)
    void polytone$readRenderListFromTree(Viewport viewport, FogParameters fogParameters);
}
