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

// Makes Polytone's data driven custom_models bakeable through the vanilla entity model pipeline.
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
