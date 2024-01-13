package net.mehvahdjukaar.polytone.forge;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraftforge.accesstransformer.MethodTarget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.RuntimeDistCleaner;

/**
 * Author: MehVahdJukaar
 */
@Mod(Polytone.MOD_ID)
public class PolytoneForge {

    public PolytoneForge() {
        if(FMLEnvironment.dist == Dist.CLIENT) {
            Polytone.init(false);

            FMLJavaModLoadingContext.get().getModEventBus().register(this);
            MinecraftForge.EVENT_BUS.addListener(PolytoneForge::onTagSync);
        }
    }

    @SubscribeEvent
    public void registerCustomResolver(RegisterColorHandlersEvent.ColorResolvers event){
        event.register(Colormap.TEMPERATURE_RESOLVER);
        event.register(Colormap.DOWNFALL_RESOLVER);
    }

    public static void onTagSync(TagsUpdatedEvent event){
        if(event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED){
            Polytone.onTagsReceived(event.getRegistryAccess());
        }
    }


}
