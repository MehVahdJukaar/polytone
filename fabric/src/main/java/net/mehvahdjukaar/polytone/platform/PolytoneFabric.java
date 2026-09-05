package net.mehvahdjukaar.polytone.platform;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionBlockStateModel;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionModel;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.particle.debug.ParticleHitboxDebugRenderer;
import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;


public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SpecialModelsHandlerImpl.init();
        CustomUnbakedBlockStateModel.register(ExpressionModel.ID, ExpressionBlockStateModel.Unbaked.CODEC);
        FabricLoader instance = FabricLoader.getInstance();
        Polytone.init(instance.isDevelopmentEnvironment(), false);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                //TODO: fix reload command here
                Polytone.onTagsReceived(registries);
            }
        });


        LevelRenderEvents.BEFORE_GIZMOS.register(
                context -> ParticleHitboxDebugRenderer.emitGizmos()
        );
        DebugScreenEntries.register(ParticleHitboxDebugRenderer.ID, new DebugEntryNoop());

        LevelRenderEvents.START_MAIN.register((context) ->
                ClientFrameTicker.onRenderTick(Minecraft.getInstance()));

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.level != null) {
                Polytone.onTick(client.level);
            }

        });


        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SlotifyScreen ss) {
                ScreenEvents.afterExtract(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) ->
                        NautilusGuiModifierOverlay.renderScreenExtras(graphics, ss, scaledWidth, scaledHeight, mouseX, mouseY, tickDelta));
            }
        });

        ItemTooltipCallback.EVENT.register((stack, c, context, lines) -> {
            var modifier = ((IPolytoneItem) stack.getItem()).polytone$getModifier();
            if (modifier != null) {
                modifier.modifyTooltips(lines);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                Polytone.onLogOut());

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                Polytone.currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                Polytone.currentServer = null);
    }

    public static MinecraftServer currentServer;

}
