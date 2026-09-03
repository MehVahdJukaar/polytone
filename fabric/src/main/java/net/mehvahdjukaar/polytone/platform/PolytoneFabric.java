package net.mehvahdjukaar.polytone.platform;

import com.google.common.base.Preconditions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.mixins.fabric.ParticleEngineAccessor;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class PolytoneFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricLoader instance = FabricLoader.getInstance();
        boolean iris = FabricLoader.getInstance().isModLoaded("iris") || FabricLoader.getInstance().isModLoaded("oculus");
        Polytone.init(instance.isDevelopmentEnvironment(), false, iris);

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                //TODO: fix reload command here
                Polytone.onTagsReceived(registries);
            }
        });
        WorldRenderEvents.START.register((context) ->
                ClientFrameTicker.onRenderTick(context.gameRenderer().getMinecraft()));

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.level != null) {
                ClientFrameTicker.onTick(client.level);
            }
        });

        WorldRenderEvents.LAST.register(context -> {
            PolytoneRenderTypes.onRenderLast();
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SlotifyScreen ss) {
                //always register, a modifier or a live preview can show up later
                ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) ->
                        SlotifyScreen.renderExtras(graphics, ss, scaledWidth, scaledHeight, mouseX, mouseY, tickDelta));
            }
        });

        ItemTooltipCallback.EVENT.register((stack, c, context, lines) -> {
            var modifier = ((IPolytoneItem) stack.getItem()).polytone$getModifier();
            if (modifier != null) {
                modifier.modifyTooltips(lines);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);

        // vanilla renders only the render types in RENDER_ORDER, so ours has to be spliced in.
        // On JOIN, not SERVER_STARTED: that one never fires when connecting to a dedicated server,
        // leaving every additive_translucent particle invisible there.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> addRenderParticlesType());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Polytone.onLoggedOut();
        });
    }

    private static boolean addedRenderParticleType = false;

    public static void addRenderParticlesType() {
        if (addedRenderParticleType) return; // appending twice would draw those particles twice
        addedRenderParticleType = true;
        List<ParticleRenderType> renderOrder = new ArrayList<>(ParticleEngineAccessor.getRENDER_ORDER());
        renderOrder.add(Preconditions.checkNotNull(PolytoneRenderTypes.PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE));
        ParticleEngineAccessor.setRENDER_ORDER(renderOrder);
    }

    public static MinecraftServer currentServer;
}
