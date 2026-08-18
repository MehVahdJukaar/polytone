package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GpuParticlesManager extends ContentManager<GpuParticleType> {

    private final Map<Identifier, GpuParticleRenderer> renderers = new LinkedHashMap<>();
    private final List<GpuParticleRenderer> pendingClose = new ArrayList<>();

    public GpuParticlesManager() {
        super(Spec.of("GPU Particle", () -> GpuParticleType.CODEC)
                .folders("gpu_particles")
                .wikiPage("GPU-Particles"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        ParticleResources particleResources = Minecraft.getInstance().particleEngine.resourceManager;
        for (var e : parseEnabledJsons(resources.jsons(), ops)) {
            Identifier id = e.getKey();
            if (BuiltInRegistries.PARTICLE_TYPE.get(id).isPresent()) {
                Polytone.LOGGER.error("GPU particle {} clashes with an existing particle type, skipping", id);
                continue;
            }
            GpuParticleRenderer renderer = new GpuParticleRenderer(id, e.getValue());
            ParticleType<ParticleOptions> type = PlatStuff.makeParticleType(false);
            PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);
            particleResources.register(type, renderer);
            renderers.put(id, renderer);
        }
        if (!renderers.isEmpty()) {
            Polytone.LOGGER.info("Registered GPU particles: {}", renderers.keySet());
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var id : renderers.keySet()) {
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
        // GL objects can only be dropped on the render thread, so the next frame does it
        pendingClose.addAll(renderers.values());
        renderers.clear();
    }

    public boolean isGpuParticle(Identifier id) {
        return renderers.containsKey(id);
    }

    // Own frame graph pass, right after vanilla's particles one, on the same target: reusing that pass
    // isn't possible since our draws are raw render passes rather than feature submissions.
    public void addRenderPass(FrameGraphBuilder builder, LevelTargetBundle targets, GpuBufferSlice shaderFog,
                              Vec3 cameraPos, long gameTime, float partialTick) {
        if (!pendingClose.isEmpty()) {
            for (GpuParticleRenderer r : pendingClose) r.close();
            pendingClose.clear();
        }
        if (renderers.isEmpty()) return;

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
        List<GpuParticleRenderer> ready = new ArrayList<>(renderers.size());
        for (GpuParticleRenderer r : renderers.values()) {
            if (r.prepare(cameraPos, gameTime, partialTick)) ready.add(r);
        }
        if (ready.isEmpty()) return;

        // vertices are already camera relative, so the transform is the plain camera view matrix
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(), new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone gpu particles", target.getColorTextureView(), OptionalInt.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            for (GpuParticleRenderer r : ready) {
                r.draw(pass);
            }
        }
    }
}
