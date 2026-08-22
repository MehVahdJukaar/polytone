package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

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

    public void addRenderPass(FrameGraphBuilder builder, LevelTargetBundle targets, GpuBufferSlice shaderFog,
                              Vec3 cameraPos, long gameTime, float partialTick) {
        if (!pendingClose.isEmpty()) {
            for (GpuParticleRenderer r : pendingClose) r.close();
            pendingClose.clear();
        }
        if (active.isEmpty()) return;

        FramePass pass = builder.addPass("polytone_gpu_particles");
        if (targets.particles != null) {
            targets.particles = pass.readsAndWrites(targets.particles);
        } else {
            targets.main = pass.readsAndWrites(targets.main);
        }
        ResourceHandle<RenderTarget> handle = targets.particles != null ? targets.particles : targets.main;
        pass.executes(() -> {
            RenderSystem.setShaderFog(shaderFog);
            render(handle.get(), cameraPos, gameTime, partialTick);
        });
    }

    private void render(RenderTarget target, Vec3 cameraPos, long gameTime, float partialTick) {
        if (heightmap == null) heightmap = new GpuParticleHeightmap();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && active.stream().anyMatch(r -> r.type().killBelowHeightmap())) {
            heightmap.update(level, cameraPos);
        }
        List<GpuParticleRenderer> ready = new ArrayList<>(active.size());
        for (GpuParticleRenderer r : active) {
            if (r.prepare(cameraPos, gameTime, partialTick, heightmap)) ready.add(r);
        }
        if (ready.isEmpty()) return;

        // vertices are already camera relative, so the transform is the plain camera view matrix
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                new Matrix4f(RenderSystem.getModelViewStack()), new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone gpu particles", target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            for (GpuParticleRenderer r : ready) {
                r.draw(pass);
            }
        }
    }
}
