package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricLoader instance = FabricLoader.getInstance();
        boolean sodiumOn = instance.isModLoaded("sodium") || instance.isModLoaded("indium");

        Polytone.init(sodiumOn, instance.isDevelopmentEnvironment(), false);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                Polytone.onTagsReceived(registries);
            }
        });
        WorldRenderEvents.START.register((context) -> {
            ClientFrameTicker.onRenderTick(context.gameRenderer().getMinecraft());
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
                ScreenModifier guiModifier = Polytone.SLOTIFY.getGuiModifier(screen);
                if (guiModifier != null && !guiModifier.sprites().isEmpty()) {
                    ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {

                        var matrices = graphics.pose();
                        matrices.pushPose();
                        matrices.setIdentity();
                        matrices.translate(scaledWidth / 2F, scaledHeight / 2F, 500);

                        ss.polytone$renderExtraSprites(graphics);
                        matrices.popPose();
                    });
                }
            }
        });

        ItemTooltipCallback.EVENT.register((stack, c, context, lines) -> {
            var modifier = ((IPolytoneItem) stack.getItem()).polytone$getModifier();
            if (modifier != null) {
                modifier.modifyTooltips(lines);
            }
        });

    }


}
