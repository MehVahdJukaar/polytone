package net.mehvahdjukaar.polytone.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Author: MehVahdJukaar
 */
@Mod(Polytone.MOD_ID)
public class PolytoneForge {

    public PolytoneForge(IEventBus bus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Polytone.init(false);

            bus.register(this);
            NeoForge.EVENT_BUS.addListener(PolytoneForge::onTagSync);
            NeoForge.EVENT_BUS.addListener(PolytoneForge::renderScreen);
        } else {
            Polytone.LOGGER.warn("Slotify has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }
    }

    @SubscribeEvent
    public void registerCustomResolver(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(ColorUtils.TEMPERATURE_RESOLVER);
        event.register(ColorUtils.DOWNFALL_RESOLVER);
    }

    public static void onTagSync(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            Polytone.onTagsReceived(event.getRegistryAccess());
        }
    }

    public static void renderScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        SlotifyScreen ss = (SlotifyScreen) screen;
        if (ss.polytone$hasSprites()) {

            PoseStack poseStack = event.getGuiGraphics().pose();
            poseStack.pushPose();
            poseStack.translate(screen.width / 2F, screen.height / 2F, 500);
            ss.polytone$renderExtraSprites(poseStack);
            poseStack.popPose();
        }
    }


}
