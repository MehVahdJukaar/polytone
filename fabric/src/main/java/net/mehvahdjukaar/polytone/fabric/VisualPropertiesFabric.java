package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.mehvahdjukaar.polytone.Polytone;

public class VisualPropertiesFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Polytone.init(true);
    }


}
