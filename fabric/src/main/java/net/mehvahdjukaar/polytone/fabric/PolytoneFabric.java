package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Polytone.init(true);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if(client) Polytone.onTagsReceived(registries);
        });



    }

    private static final     SoundEvent TEST = Registry.register(BuiltInRegistries.SOUND_EVENT,
            "test", SoundEvent.createVariableRangeEvent(new ResourceLocation(
                    ("block.amethyst_cluster.break"))));


}
