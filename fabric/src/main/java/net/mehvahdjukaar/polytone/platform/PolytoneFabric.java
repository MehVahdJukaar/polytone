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
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionBlockStateModel;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionModel;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.particle.debug.ParticleHitboxDebugRenderer;
import net.mehvahdjukaar.polytone.content.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.mixins.fabric.ParticleEngineAccessor;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        addToProfiles();

        LevelRenderEvents.START_MAIN.register((context) ->
                ClientFrameTicker.onRenderTick(context.gameRenderer().getMinecraft()));

        LevelRenderEvents.END_MAIN.register(context ->
                PolytoneRenderTypes.onRenderLast());

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            PolytoneRenderTypes.cacheMatrices(); //might not be enough. needs to be after particles but we dont have it
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.level != null) {
                Polytone.onTick(client.level);
            }

        });


        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SlotifyScreen ss) {
                ScreenModifier guiModifier = Polytone.SLOTIFY.getGuiModifier(screen);
                if (guiModifier != null && !guiModifier.extraRenderables().isEmpty()) {
                    ScreenEvents.afterExtract(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {

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

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                Polytone.onLogOut());

        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                addRenderParticlesType());
    }

    private static void addToProfiles() {
        var old = DebugScreenEntries.PROFILES;
        var def = old.get(DebugScreenProfile.DEFAULT);
        var newDef = new HashMap<>(def);
        newDef.put(ParticleHitboxDebugRenderer.ID, DebugScreenEntryStatus.IN_OVERLAY);
        var perf = old.get(DebugScreenProfile.PERFORMANCE);
        var newPerf = new HashMap<>(perf);
        newPerf.put(ParticleHitboxDebugRenderer.ID, DebugScreenEntryStatus.IN_OVERLAY);
        var newProfiles = new HashMap<>(old);
        newProfiles.put(DebugScreenProfile.DEFAULT, newDef);
        newProfiles.put(DebugScreenProfile.PERFORMANCE, newPerf);
        DebugScreenEntries.PROFILES = newProfiles;
    }

    public static MinecraftServer currentServer;

    public static void addRenderParticlesType() {
        List<ParticleRenderType> renderOrder = new ArrayList<>(ParticleEngineAccessor.getRENDER_ORDER());
        renderOrder.add(PolytoneRenderTypes.PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE.get());
        ParticleEngineAccessor.setRENDER_ORDER(renderOrder);
    }

}
