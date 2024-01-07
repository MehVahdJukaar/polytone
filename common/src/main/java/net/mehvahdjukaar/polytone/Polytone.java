package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.polytone.colors.ColorManager;
import net.mehvahdjukaar.polytone.properties.BlockPropertiesManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Polytone {
    public static final String MOD_ID = "polytone";

    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    public static void init(boolean fabric) {

        PlatStuff.addClientReloadListener(ColorManager::new, res("color_manager"));
        PlatStuff.addClientReloadListener(BlockPropertiesManager::new, res("block_properties_manager"));

    }

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }


}
