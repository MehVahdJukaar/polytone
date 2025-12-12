package net.mehvahdjukaar.polytone.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModelStuffImpl {

    //DUMB
    private static final Map<Identifier, StandaloneModelKey<QuadCollection>> SPECIAL_MODELS = new HashMap<>();

    public static void clear() {
        SPECIAL_MODELS.clear();
    }

    public static void addSpecialModel(Identifier id) {
        SPECIAL_MODELS.put(id, new StandaloneModelKey<>(new ModelDebugName() {
            @Override
            public String debugName() {
                return id.toString();
            }
        }));

    }

    @Nullable
    public static QuadCollection getSpecialModel(Identifier id) {
        var key = SPECIAL_MODELS.get(id);
        if (key != null) {
            ModelManager mm = Minecraft.getInstance().getModelManager();
            return mm.getStandaloneModel(key);
        }
        return null;
    }

    public static void init(IEventBus bus) {
        bus.addListener(ModelStuffImpl::registerExtraModels);
    }

    public static void registerExtraModels(ModelEvent.RegisterStandalone event) {
        for (var entry : SPECIAL_MODELS.entrySet()) {
            event.register(entry.getValue(), SimpleUnbakedStandaloneModel.quadCollection(entry.getKey()));
        }
    }
}
