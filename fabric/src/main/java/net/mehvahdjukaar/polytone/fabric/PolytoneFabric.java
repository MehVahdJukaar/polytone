package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.impl.client.rendering.ColorProviderRegistryImpl;
import net.fabricmc.fabric.mixin.client.rendering.BlockColorsMixin;
import net.mehvahdjukaar.polytone.Polytone;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Polytone.init(true);
    }


}
