package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        boolean sodiumOn = FabricLoader.getInstance().isModLoaded("sodium");
        Polytone.init(sodiumOn);

        if (sodiumOn) {
            Polytone.LOGGER.error("!!!!!");
            Polytone.LOGGER.error("SODIUM has been detected. POLYTONE colormaps will be OFF. This is a SODIUM issue and CANNOT be fixed by me until Sodium merges this PR https://github.com/CaffeineMC/sodium-fabric/pull/2222");
            Polytone.LOGGER.error("!!!!!");
        }

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) Polytone.onTagsReceived(registries);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SlotifyScreen ss) {
                ScreenModifier guiModifier = GuiModifierManager.getGuiModifier(screen);
                if (guiModifier != null && !guiModifier.sprites().isEmpty()) {
                    ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {

                        var matrices = graphics.pose();
                        matrices.pushPose();
                        matrices.setIdentity();
                        matrices.translate(scaledWidth / 2F, scaledHeight / 2F, 500);

                        ss.polytone$renderExtraSprites(matrices);
                        matrices.popPose();
                    });
                }
            }
        });
    }

}
