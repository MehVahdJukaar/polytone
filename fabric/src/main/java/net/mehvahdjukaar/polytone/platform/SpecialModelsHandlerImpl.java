package net.mehvahdjukaar.polytone.platform;

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

    private static ModelLoadingPlugin.Context hack = null;

    public static void init() {
        // safely sets hack
        ModelLoadingPlugin.register(context -> {
            hack = context;
        });
    }

    public static void finalizeAdditions() {
        if (hack == null) return;
        // Wait for hack to be initialized, up to a timeout if desired
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


    }

}
