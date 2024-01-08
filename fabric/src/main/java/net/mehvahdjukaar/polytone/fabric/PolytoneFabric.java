package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.mehvahdjukaar.polytone.Polytone;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Polytone.init(true);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if(client) Polytone.onTagsReceived(registries);
        });
    }


}
