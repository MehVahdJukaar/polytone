package net.mehvahdjukaar.polytone.content.common.expressions;

import hollowpoint.nexp.api.ExpScope;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.content.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.CameraProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.GlobalProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.PlayerProxy;

public class ExpUtils {

    public static ExpScope commonScope() {
        return new ExpScope()
                .supplied(CameraProxy.class, () -> {
                    SimProxies sim = PreviewContext.active();
                    return sim == null ? CameraProxy.INSTANCE : sim.camera;
                }, "camera", "c")
                .supplied(GlobalProxy.class, () -> {
                    SimProxies sim = PreviewContext.active();
                    return sim == null ? GlobalProxy.INSTANCE : sim.global;
                }, "global", "g")
                .supplied(PlayerProxy.class, () -> {
                    SimProxies sim = PreviewContext.active();
                    return sim == null ? PlayerProxy.INSTANCE : sim.player;
                }, "player", "p");
    }

    //stub for forward compat
    public static Object parseEnvAttr(String attributeName) {
        return null;
    }
}
