package net.mehvahdjukaar.polytone.platform;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SpecialModelsHandlerImpl {

    //DUMB
    private static final Map<Identifier, ExtraModelKey<QuadCollection>> SPECIAL_MODELS = new HashMap<>();

    private static CompletableFuture<Map<Identifier, ExtraModelKey<QuadCollection>>> pendingModels = new CompletableFuture<>();

    public static void clear() {
        SPECIAL_MODELS.clear();
    }

    public static void addSpecialModel(Identifier id) {
        SPECIAL_MODELS.put(id, ExtraModelKey.create(id::toString));
    }

    @Nullable
    public static QuadCollection getSpecialModel(Identifier id) {
        var key = SPECIAL_MODELS.get(id);
        if (key != null) {
            var mm = Minecraft.getInstance().getModelManager();
            return mm.getModel(key);
        }
        return null;
    }

    public static void init() {
        PreparableModelLoadingPlugin.register((sharedState, executor) -> {
            pendingModels = new CompletableFuture<>();
            return pendingModels;
        }, (models, context) -> {
            for (var entry : models.entrySet()) {
                context.addModel(entry.getValue(), new SimpleUnbakedExtraModel<>(
                        entry.getKey(),
                        (model, baker) -> model.bakeTopGeometry(
                                model.getTopTextureSlots(),
                                baker,
                                BlockModelRotation.IDENTITY
                        )
                ));
            }
        });
    }

    public static void finalizeAdditions() {
        pendingModels.complete(Map.copyOf(SPECIAL_MODELS));
    }

}
