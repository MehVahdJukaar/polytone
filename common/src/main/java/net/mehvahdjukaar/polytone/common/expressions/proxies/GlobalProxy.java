package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.FabricSeasonsCompat;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.mehvahdjukaar.polytone.compat.SereneSeasonsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@BeanGettersAliases
public class GlobalProxy {

    public static final GlobalProxy INSTANCE = new GlobalProxy();

    @NotNull
    private Level delegate(){
        return Minecraft.getInstance().level;
    }

    public long time(){
        return delegate().getGameTime();
    }

    public String season(){
      return ISeason.get(delegate()).lowercaseName();
    }

    public float seasonNumber(){
        return ISeason.getNumber(delegate());
    }
}
