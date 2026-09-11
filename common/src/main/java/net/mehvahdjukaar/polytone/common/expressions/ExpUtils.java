package net.mehvahdjukaar.polytone.common.expressions;

import hollowpoint.nexp.api.ExpScope;
import net.mehvahdjukaar.polytone.common.expressions.preview.PreviewContext;
import net.mehvahdjukaar.polytone.common.expressions.preview.SimProxies;
import net.mehvahdjukaar.polytone.common.expressions.proxies.CameraProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.GlobalProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.PlayerProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttribute;
import org.jspecify.annotations.NonNull;

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

    public static @NonNull EnvironmentAttribute<?> parseEnvAttr(String attributeName) {
        EnvironmentAttribute<?> attr = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.getValue(Identifier.parse(attributeName));
        if (attr == null) {
            throw new IllegalArgumentException("Unknown environment attribute: " + attributeName);
        }
        return attr;
    }
}
