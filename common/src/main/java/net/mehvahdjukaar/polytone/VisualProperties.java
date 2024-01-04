package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.polytone.moonlight_configs.ConfigBuilder;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigSpec;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class VisualProperties {
    public static final String MOD_ID = "polytone";

    public static final Logger LOGGER = LogManager.getLogger("Visual Properties");


    public static Supplier<Double> expansion;
    public static Supplier<Double> indent;
    public static Supplier<Double> shrinkMult = () -> 0d;
    public static ConfigSpec config;

    public static void init(boolean fabric) {

        ConfigBuilder builder = ConfigBuilder.create(res("client"), ConfigType.CLIENT);

        builder.push("default");
        builder.pop();


        config = builder.buildAndRegister();

        PlatStuff.addClientReloadListener(MapColorManager::new, res("color_manager"));
        PlatStuff.addClientReloadListener(ClientBlockPropertiesManager::new, res("block_properties_manager"));

    }

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }


    public static Screen makeScreen(Screen screen) {
        return config.makeScreen(screen);
    }

}
