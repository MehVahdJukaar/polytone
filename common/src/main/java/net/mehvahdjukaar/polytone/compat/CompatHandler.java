package net.mehvahdjukaar.polytone.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.PlatStuff;

public class CompatHandler {

    public static final boolean SS = PlatStuff.isModLoaded("sereneseasons");
    public static final boolean IRIS  =  PlatStuff.isModLoaded("iris") ||  PlatStuff.isModLoaded("oculus");

}
