package net.mehvahdjukaar.visualprop.forge;

import net.mehvahdjukaar.visualprop.VisualProperties;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MehVahdJukaar
 */
@Mod(VisualProperties.MOD_ID)
public class VisualPropertiesForge {

    public VisualPropertiesForge() {
        VisualProperties.init(false);
    }


}
