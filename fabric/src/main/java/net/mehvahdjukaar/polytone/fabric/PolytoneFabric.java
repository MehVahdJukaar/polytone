package net.mehvahdjukaar.polytone.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.mixins.fabric.ParticleEngineAccessor;
import net.mehvahdjukaar.polytone.content.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.misc.ClientFrameTicker;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SpecialModelsHandlerImpl.init();
        FabricLoader instance = FabricLoader.getInstance();
        Polytone.init(instance.isDevelopmentEnvironment(), false);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                //TODO: fix reload command here
                Polytone.onTagsReceived(registries);
            }
        });
        WorldRenderEvents.START_MAIN.register((context) ->
                ClientFrameTicker.onRenderTick(context.gameRenderer().getMinecraft()));

        WorldRenderEvents.END_MAIN.register(context -> {
            PolytoneRenderTypes.onRenderLast();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PolytoneRenderTypes.cacheMatrices(); //might not be enough. needs to be after particles but we dont have it
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.level != null) {
                ClientFrameTicker.onTick(client.level);
            }

        });


        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SlotifyScreen ss) {
                ScreenModifier guiModifier = Polytone.SLOTIFY.getGuiModifier(screen);
                if (guiModifier != null && !guiModifier.extraRenderables().isEmpty()) {
                    ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {

                        var matrices = graphics.pose();
                        matrices.pushMatrix();
                        matrices.identity();
                        matrices.translate(scaledWidth / 2F, scaledHeight / 2F);

                        ss.polytone$renderExtraSprites(graphics, mouseX, mouseY, tickDelta);
                        matrices.popMatrix();
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

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Polytone.onLogOut();
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            addRenderParticlesType();
        });
    }

    public static MinecraftServer currentServer;

    public static void addRenderParticlesType() {
        List<ParticleRenderType> renderOrder = new ArrayList<>(ParticleEngineAccessor.getRENDER_ORDER());
        renderOrder.add(PolytoneRenderTypes.PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE.get());
        ParticleEngineAccessor.setRENDER_ORDER(renderOrder);
    }

}
