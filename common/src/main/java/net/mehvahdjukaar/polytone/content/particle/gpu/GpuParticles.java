package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class GpuParticles {

    private final List<GpuParticleRenderer> active = new ArrayList<>();
    // GL objects can only be dropped on the render thread, so the next frame does it
    private final List<GpuParticleRenderer> pendingClose = new ArrayList<>();
    // shared by every type; only refreshed while a type asks for it
    private @Nullable GpuParticleHeightmap heightmap;

    public void add(GpuParticleRenderer renderer) {
        active.add(renderer);
    }

    public void retireAll() {
        pendingClose.addAll(active);
        active.clear();
    }

    public void render(Camera camera, GameRenderer gameRenderer, LightTexture lightTexture,
                       Matrix4f modelView, Matrix4f projection, float partialTick) {
        if (!pendingClose.isEmpty()) {
            for (GpuParticleRenderer r : pendingClose) r.close();
            pendingClose.clear();
        }
        if (active.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Vec3 cameraPos = camera.getPosition();
        if (heightmap == null) heightmap = new GpuParticleHeightmap();
        if (active.stream().anyMatch(r -> r.type().killBelowHeightmap())) {
            heightmap.update(level, cameraPos);
        }

        boolean foggy = level.effects().isFoggyAt(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y))
                || mc.gui.getBossOverlay().shouldCreateWorldFog();
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN,
                Math.max(gameRenderer.getRenderDistance(), 32f), foggy, partialTick);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        lightTexture.turnOnLightLayer();
        try {
            for (GpuParticleRenderer r : active) {
                r.render(mc, cameraPos, level.getGameTime(), partialTick, modelView, projection, heightmap);
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
