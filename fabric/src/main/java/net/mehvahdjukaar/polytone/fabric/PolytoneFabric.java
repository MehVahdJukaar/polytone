package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricLoader instance = FabricLoader.getInstance();
        boolean sodiumOn = instance.isModLoaded("sodium") || instance.isModLoaded("indium");
        Polytone.init(sodiumOn);

        if (sodiumOn) {
            Polytone.LOGGER.error("!!!!!");
            Polytone.LOGGER.error("SODIUM has been detected. POLYTONE colormaps will be OFF. This is a SODIUM issue and CANNOT be fixed by me until Sodium merges this PR https://github.com/CaffeineMC/sodium-fabric/pull/2222");
            Polytone.LOGGER.error("!!!!!");
        }

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) Polytone.onTagsReceived(registries);
        });

    }

}
