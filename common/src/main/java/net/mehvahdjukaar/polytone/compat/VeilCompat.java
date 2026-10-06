package net.mehvahdjukaar.polytone.compat;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.mehvahdjukaar.polytone.content.shaders.light.ResolvedPointLight;
import net.mehvahdjukaar.polytone.content.shaders.light.PointLightStorage;

import java.util.HashMap;
import java.util.Map;

public class VeilCompat {

    public static PointLightStorage createPointLightStorage() {
        return new VeilPointLightStorage();
    }

    private static class VeilPointLightStorage implements PointLightStorage {

        private final Map<Object, LightRenderHandle<PointLightData>> handles = new HashMap<>();

        @Override
        public void set(Object key, double x, double y, double z, ResolvedPointLight light) {
            LightRenderHandle<PointLightData> handle = handles.get(key);
            if (handle != null && handle.isValid()) {
                configure(handle.getLightData(), x, y, z, light);
            } else {
                PointLightData data = new PointLightData();
                configure(data, x, y, z, light);
                handles.put(key, VeilRenderSystem.renderer().getLightRenderer().addLight(data));
            }
        }

        @Override
        public void move(Object key, double x, double y, double z) {
            LightRenderHandle<PointLightData> handle = handles.get(key);
            if (handle != null && handle.isValid()) handle.getLightData().setPosition(x, y, z);
        }

        @Override
        public void remove(Object key) {
            LightRenderHandle<PointLightData> handle = handles.remove(key);
            if (handle != null && handle.isValid()) {
                handle.free();
            }
        }

        @Override
        public void clear() {
            for (var handle : handles.values()) {
                if (handle.isValid()) handle.free();
            }
            handles.clear();
        }

        private static void configure(PointLightData data, double x, double y, double z, ResolvedPointLight light) {
            data.setPosition(x, y, z)
                    .setRadius(light.radius())
                    .setBrightness(light.brightness())
                    .setColor(light.color());
        }
    }
}
