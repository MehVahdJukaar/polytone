package net.mehvahdjukaar.polytone.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Author: MehVahdJukaar
 */
@Mod("polytone")
public class PolytoneForge {
    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    static IEventBus bus;

    public PolytoneForge(IEventBus modBus) {
        bus = modBus;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            boolean iris = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
            Polytone.init(!FMLEnvironment.production, true, iris);
            NeoForge.EVENT_BUS.register(this);
        } else {
            LOGGER.warn("Polytone has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }

    }

    //@SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Polytone.ENTITY_MODIFIERS.onEntityTick(event.getEntity());
    }

    @SubscribeEvent
    public void onTick(LevelTickEvent.Pre event) {
        ClientFrameTicker.onTick(event.getLevel());
    }

    @SubscribeEvent
    public void onRender(RenderFrameEvent.Pre onRender) {
        ClientFrameTicker.onRenderTick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent tooltipEvent) {
        var mod = ((IPolytoneItem) tooltipEvent.getItemStack().getItem()).polytone$getModifier();
        if (mod != null) {
            mod.modifyTooltips(tooltipEvent.getToolTip());
        }
    }

    @SubscribeEvent
    public void onTagSync(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            Polytone.onTagsReceived(event.getRegistryAccess());
        }
        bus = null;
    }

    @SubscribeEvent
    public void renderStageEventAfterLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL ) {
            PolytoneRenderTypes.onRenderLast();
        }
    }

    @SubscribeEvent
    public void renderScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        SlotifyScreen ss = (SlotifyScreen) screen;
        if (ss.polytone$hasSprites()) {

            GuiGraphics graphics = event.getGuiGraphics();
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(screen.width / 2F, screen.height / 2F, 500);
            ss.polytone$renderExtraSprites(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            poseStack.popPose();
        }
    }


    @SubscribeEvent
    public void fogEvent(ViewportEvent.RenderFog fogEvent) {
        if (fogEvent.getType() != FogType.NONE || fogEvent.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        Vec2 targetFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(fogEvent.getNearPlaneDistance(), fogEvent.getFarPlaneDistance());
        if (targetFog != null) {
            fogEvent.setNearPlaneDistance(targetFog.x);
            fogEvent.setFarPlaneDistance(targetFog.y);
            fogEvent.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Polytone.onLoggedOut();
    }

}
