package net.mehvahdjukaar.polytone.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Author: MehVahdJukaar
 */
@Mod(Polytone.MOD_ID)
public class PolytoneForge {

    public PolytoneForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Polytone.init(false);

            FMLJavaModLoadingContext.get().getModEventBus().register(this);
            MinecraftForge.EVENT_BUS.addListener(PolytoneForge::onTagSync);
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



}
