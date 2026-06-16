package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
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
 * Rather than cancelling {@code bakeLayer} (which would short circuit the method before its {@code RETURN}, where
 * model replacing mods like EMF inject), we lazily insert our {@link LayerDefinition}s into the {@code roots} map and
 * let the original method run normally. This way our reference models bake through the exact same path as vanilla
 * layers and EMF gets to wrap them. The map is copied into a mutable, thread safe one in the constructor since vanilla
 * builds it as an immutable map and layer baking can happen off thread during reloads.
 */
@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin {

    @Mutable
    @Shadow
    @Final
    private Map<ModelLayerLocation, LayerDefinition> roots;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polytone$mutableRoots(Map<ModelLayerLocation, LayerDefinition> roots, CallbackInfo ci) {
        this.roots = new ConcurrentHashMap<>(roots);
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
