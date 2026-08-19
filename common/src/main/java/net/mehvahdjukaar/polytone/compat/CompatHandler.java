package net.mehvahdjukaar.polytone.compat;

import net.mehvahdjukaar.polytone.PlatStuff;

public class CompatHandler {

    public static final boolean SS = PlatStuff.isModLoaded("sereneseasons");
    public static final boolean FS = PlatStuff.isModLoaded("fabric_seasons");
    public static final boolean IRIS  =  PlatStuff.isModLoaded("iris") ||  PlatStuff.isModLoaded("oculus");
    public static final boolean ALEX_CAVES = PlatStuff.isModLoaded("alexscaves");
    public static final boolean EMF = PlatStuff.isModLoaded("entity_model_features");
    public static final boolean ETF = PlatStuff.isModLoaded("entity_texture_features");
    public static final boolean SODIUM = PlatStuff.isModLoaded("sodium");
    public static final boolean NAUTILUS = PlatStuff.isModLoaded("nautilus_studio");

}
