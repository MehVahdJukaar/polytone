package net.mehvahdjukaar.polytone.mixins.accessor;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

// reaches sodium's private renderSectionManager so the shadow pass can re-cull its terrain render
// list against the light volume. @Pseudo, so it simply doesn't apply when sodium is absent
@Pseudo
@Mixin(SodiumWorldRenderer.class)
public interface SodiumWorldRendererShadowAccessor {

    @Accessor(value = "renderSectionManager", remap = false)
    RenderSectionManager polytone$getRenderSectionManager();
}
