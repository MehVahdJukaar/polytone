package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.mehvahdjukaar.polytone.VisualProperties;

public class VisualPropertiesFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VisualProperties.init(true);
    }


}
