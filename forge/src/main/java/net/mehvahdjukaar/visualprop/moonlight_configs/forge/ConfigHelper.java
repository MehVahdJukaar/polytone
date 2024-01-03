package net.mehvahdjukaar.visualprop.moonlight_configs.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigHelper {

    /**
     * Loads a config file from the specified directory and assigns it to the given spec
     *
     * @param targetSpec target config spec
     * @param fileName   name of the config file
     * @param addToMod   if true it will add the config file to the mod container aswell
     * @return created mod config. Used for tracking and events
     */
    public static ModConfig addAndLoadConfigFile(ForgeConfigSpec targetSpec, String fileName, boolean addToMod) {
       // loadConfigFile(fileName, targetSpec);

        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        ModConfig config = new ModConfig(ModConfig.Type.COMMON, targetSpec, modContainer, fileName);

        if (addToMod) modContainer.addConfig(config);

        return config;
    }



    public static void reloadConfigFile(ModConfig config) {
       // loadConfigFile(config.getFileName(), (ForgeConfigSpec) config.getSpec());
    }


}
