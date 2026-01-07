package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.compat.ISeason;
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

    public double time(){
        return delegate().getGameTime();
    }

    public String season(){
      return ISeason.get(delegate()).lowercaseName();
    }

    public float seasonNumber(){
        return ExpTicker.getSeasonNumber();
    }

    public int renderDistance(){
        return Minecraft.getInstance().options.renderDistance().get();
    }

    public double rain(){
        return ExpTicker.getRainAndThunder();
    }

    public Object environmentAttribute(String value){
        var a = ExpUtils.parseEnvAttr(value);
        return delegate().environmentAttributes().getDimensionValue(a);
    }
}
