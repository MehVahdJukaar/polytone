package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Reaches the full section grid for the shadow-map generator, which replays already-compiled chunk geometry
// from the light's point of view: the shadow pass iterates every compiled section and culls against the LIGHT
// volume, not the camera frustum, so occluders behind/beside the player still cast shadows (no popping when
// turning).
@Mixin(LevelRenderer.class)
public interface LevelRendererShadowAccessor {

    @Accessor("viewArea")
    @Nullable
    ViewArea polytone$getViewArea();
}
