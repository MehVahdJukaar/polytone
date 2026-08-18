package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GpuParticlesManager extends JsonPartialReloader<GpuParticleType> {

    private final Map<ResourceLocation, GpuParticleRenderer> renderers = new LinkedHashMap<>();
    private final List<GpuParticleRenderer> pendingClose = new ArrayList<>();

    public GpuParticlesManager() {
        super(Spec.of("GPU Particle", () -> GpuParticleType.CODEC)
                .folders("gpu_particles")
                .wikiPage("GPU-Particles"));
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        for (var e : parseEnabledJsons(jsons, ops)) {
            ResourceLocation id = e.getKey();
            if (BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
                Polytone.LOGGER.error("GPU particle {} clashes with an existing particle type, skipping", id);
                continue;
            }
            GpuParticleRenderer renderer = new GpuParticleRenderer(id, e.getValue());
            ParticleType<ParticleOptions> type = PlatStuff.makeParticleType(false);
            PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);
            engine.register(type, renderer);
            renderers.put(id, renderer);
        }
        if (!renderers.isEmpty()) {
            Polytone.LOGGER.info("Registered GPU particles: {}", renderers.keySet());
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var id : renderers.keySet()) {
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
        pendingClose.addAll(renderers.values());
        renderers.clear();
    }

    public boolean isGpuParticle(ResourceLocation id) {
        return renderers.containsKey(id);
    }

    public void render(Camera camera, GameRenderer gameRenderer, LightTexture lightTexture,
                       Matrix4f modelView, Matrix4f projection, float partialTick) {
        if (!pendingClose.isEmpty()) {
            for (GpuParticleRenderer r : pendingClose) r.close();
            pendingClose.clear();
        }
        if (renderers.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Vec3 cameraPos = camera.getPosition();
        boolean foggy = level.effects().isFoggyAt(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y))
                || mc.gui.getBossOverlay().shouldCreateWorldFog();
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN,
                Math.max(gameRenderer.getRenderDistance(), 32f), foggy, partialTick);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        lightTexture.turnOnLightLayer();
        try {
            for (GpuParticleRenderer r : renderers.values()) {
                r.render(mc, cameraPos, level.getGameTime(), partialTick, modelView, projection);
            }
        } finally {
            lightTexture.turnOffLightLayer();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            FogRenderer.setupNoFog();
        }
    }
}
