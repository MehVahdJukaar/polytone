package net.mehvahdjukaar.polytone.forge;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.properties.Colormap;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Author: MehVahdJukaar
 */
@Mod(Polytone.MOD_ID)
public class PolytoneForge {

    public PolytoneForge() {
        Polytone.init(false);
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void registerCustomResolver(RegisterColorHandlersEvent.ColorResolvers event){
        event.register(Colormap.TEMPERATURE_RESOLVER);
        event.register(Colormap.DOWNFALL_RESOLVER);
    }


}
