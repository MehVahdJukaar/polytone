package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoaderHooks;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoadingEventDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.texture.VariantTextureManager;

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
