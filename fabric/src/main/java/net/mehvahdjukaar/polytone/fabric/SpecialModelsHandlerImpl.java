package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpecialModelsHandlerImpl {

    //DUMB
    private static final Map<Identifier, ExtraModelKey<QuadCollection>> SPECIAL_MODELS = new HashMap<>();

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
    private static final CompletableFuture<ModelLoadingPlugin.Context> hackFuture = new CompletableFuture<>();

    public static void init() {
        // safely sets hack
        ModelLoadingPlugin.register(hackFuture::complete);
    }

    public static void finalizeAdditions() {
        try {
            // Wait for hack to be initialized, up to a timeout if desired
            ModelLoadingPlugin.Context hack = hackFuture.get(); // blocks until set

            for (var entry : SPECIAL_MODELS.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();

                hack.addModel(value, new SimpleUnbakedExtraModel<>(
                        key,
                        (model, baker) -> model.bakeTopGeometry(
                                model.getTopTextureSlots(),
                                baker,
                                BlockModelRotation.IDENTITY
                        )
                ));
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to initialize hack context", e);
        }
    }

}
