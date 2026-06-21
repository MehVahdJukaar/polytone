package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes Polytone's data driven {@code custom_models} bakeable through the vanilla entity model pipeline.
 * <p>
 * On 1.21.1 the {@code roots} map is rebuilt in {@code onResourceManagerReload} (vanilla assigns an immutable map),
 * so we replace it with a mutable, thread safe copy at the end of reload, then lazily insert our
 * {@link LayerDefinition}s into it inside {@code bakeLayer}. This way our reference models bake through the exact
 * same path as vanilla layers (so model replacing mods like EMF can wrap them) instead of being short circuited.
 */
@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin {

    @Shadow
    private Map<ModelLayerLocation, LayerDefinition> roots;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void polytone$mutableRoots(ResourceManager resourceManager, CallbackInfo ci) {
        this.roots = new ConcurrentHashMap<>(this.roots);
    }

    @Inject(method = "bakeLayer", at = @At("HEAD"))
    private void polytone$registerCustomLayer(ModelLayerLocation loc, CallbackInfoReturnable<ModelPart> cir) {
        if (!roots.containsKey(loc)) {
            LayerDefinition def = Polytone.CUSTOM_MODELS.getLayers().get(loc);
            if (def != null) {
                roots.put(loc, def);
            }
        }
    }
}
