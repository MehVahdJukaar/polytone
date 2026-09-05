//veil has no 26.2 build yet so this whole thing is parked. uncomment it, the mixin json entries
//and the modifier `colored_light` sugar once veil ships one
/*
package net.mehvahdjukaar.polytone.compat;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.content.light.LightProperties;

public class VeilCompat {

    private static final Long2ObjectMap<LightRenderHandle<PointLightData>> BLOCKS = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectMap<LightRenderHandle<PointLightData>> ENTITIES = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectMap<LightRenderHandle<PointLightData>> PARTICLES = new Long2ObjectOpenHashMap<>();

    public static void setBlockLight(long key, double x, double y, double z, LightProperties properties) {
        set(BLOCKS, key, x, y, z, properties);
    }

    public static void setEntityLight(long key, double x, double y, double z, LightProperties properties) {
        set(ENTITIES, key, x, y, z, properties);
    }

    public static void setParticleLight(long key, double x, double y, double z, LightProperties properties) {
        set(PARTICLES, key, x, y, z, properties);
    }

    public static void moveEntityLight(long key, double x, double y, double z) {
        move(ENTITIES, key, x, y, z);
    }

    public static void moveParticleLight(long key, double x, double y, double z) {
        move(PARTICLES, key, x, y, z);
    }

    private static void move(Long2ObjectMap<LightRenderHandle<PointLightData>> handles, long key,
                             double x, double y, double z) {
        LightRenderHandle<PointLightData> handle = handles.get(key);
        if (handle != null && handle.isValid()) handle.getLightData().setPosition(x, y, z);
    }

    public static void removeParticleLight(long key) {
        remove(PARTICLES, key);
    }

    public static void clearParticleLights() {
        clear(PARTICLES);
    }

    public static void removeBlockLight(long key) {
        remove(BLOCKS, key);
    }

    public static void removeEntityLight(long key) {
        remove(ENTITIES, key);
    }

    public static void clearBlockLights() {
        clear(BLOCKS);
    }

    public static void clearEntityLights() {
        clear(ENTITIES);
    }

    public static void clearAll() {
        clear(BLOCKS);
        clear(ENTITIES);
        clear(PARTICLES);
    }

    private static void set(Long2ObjectMap<LightRenderHandle<PointLightData>> handles, long key,
                            double x, double y, double z, LightProperties properties) {
        LightRenderHandle<PointLightData> handle = handles.get(key);
        if (handle != null && handle.isValid()) {
            configure(handle.getLightData(), x, y, z, properties);
        } else {
            PointLightData data = new PointLightData();
            configure(data, x, y, z, properties);
            handles.put(key, VeilRenderSystem.renderer().getLightRenderer().addLight(data));
        }
    }

    private static void configure(PointLightData data, double x, double y, double z, LightProperties properties) {
        data.setPosition(x, y, z)
                .setRadius(properties.radius())
                .setBrightness(properties.brightness())
                .setColor(properties.color());
    }

    private static void remove(Long2ObjectMap<LightRenderHandle<PointLightData>> handles, long key) {
        LightRenderHandle<PointLightData> handle = handles.remove(key);
        if (handle != null && handle.isValid()) {
            handle.free();
        }
    }

    private static void clear(Long2ObjectMap<LightRenderHandle<PointLightData>> handles) {
        for (var handle : handles.values()) {
            if (handle.isValid()) handle.free();
        }
        handles.clear();
    }
}

*/
