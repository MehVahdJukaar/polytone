package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Hooks for the shadow-map generator, which replays already-compiled chunk geometry from the
 * light's point of view. Two paths, used by {@code ShadowMapManager}:
 *
 * <ul>
 *   <li>{@code viewArea} - the full section grid. Preferred (vanilla pipeline): the shadow pass
 *       iterates every compiled section and culls against the LIGHT volume, not the camera frustum,
 *       so occluders behind/beside the player still cast shadows (no popping when turning).</li>
 *   <li>{@code renderSectionLayer} - fallback when the vanilla grid holds no compiled sections,
 *       i.e. when Sodium is installed: Sodium {@code @Overwrite}s this method and forwards the
 *       passed matrices to its own chunk renderer ({@code drawChunkLayer}), so invoking it with the
 *       light matrices replays Sodium's terrain into the shadow framebuffer. Sodium's render lists
 *       are camera-culled, so off-screen occluders are still missed on this path.</li>
 * </ul>
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererShadowAccessor {

    @Accessor("viewArea")
    @Nullable
    ViewArea polytone$getViewArea();

    @Invoker("renderSectionLayer")
    void polytone$renderSectionLayer(RenderType renderType, double x, double y, double z,
                                     Matrix4f frustumMatrix, Matrix4f projectionMatrix);
}
