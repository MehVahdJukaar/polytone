package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricLoader instance = FabricLoader.getInstance();
        boolean sodiumOn = instance.isModLoaded("sodium") || instance.isModLoaded("indium");
        Polytone.init(sodiumOn);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            Polytone.onTagsReceived(registries);
        });
        /*
        ModelLoadingPlugin.register(pluginContext -> {
           pluginContext.modifyModelAfterBake().register((model, context) -> {
               Polytone.VARIANT_TEXTURES.maybeModifyModel(model, context.id());
               return model;
           });
        });*/

    }

}
