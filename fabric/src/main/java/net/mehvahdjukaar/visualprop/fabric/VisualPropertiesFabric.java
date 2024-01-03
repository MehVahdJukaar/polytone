package net.mehvahdjukaar.visualprop.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.mehvahdjukaar.visualprop.VisualProperties;

public class VisualPropertiesFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VisualProperties.init(true);
    }


}
