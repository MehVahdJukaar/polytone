package net.mehvahdjukaar.polytone.content.particle.custom.render;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticles;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.AABB;

public class ModelParticleRenderGroup extends ParticleGroup<CustomParticleInstance> {

    final ModelParticleRenderState particleTypeRenderState = new ModelParticleRenderState();

    public ModelParticleRenderGroup(ParticleEngine particleEngine) {
        super(particleEngine);
    }


    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float f) {
        if (Polytone.CONFIGS.particlesOffThread.get() && !this.particles.isEmpty()) {
            // build the render state on a worker thread; joined lazily in ModelParticleRenderState#submit/clear,
            // so it overlaps the rest of the level render
            this.particleTypeRenderState.setExtractionFuture(
                    PolytoneAsyncParticles.submitRender(() -> extractAll(frustum, camera, f)));
        } else {
            extractAll(frustum, camera, f);
        }
        return this.particleTypeRenderState;
    }

    private void extractAll(Frustum frustum, Camera camera, float f) {
        for (CustomParticleInstance particle : this.particles) {
            if (particleInFrustum(frustum, particle, camera)) {
                try {
                    particle.extractModel(this.particleTypeRenderState, camera, f);
                } catch (Throwable var9) {
                    CrashReport crashReport = CrashReport.forThrowable(var9, "Rendering Model Particle");
                    CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
                    crashReportCategory.setDetail("Particle", particle::toString);
                    throw new ReportedException(crashReport);
                }
            }
        }
    }


    public static boolean particleInFrustum(Frustum frustum, Particle particle, Camera camera) {
        AABB bb = particle.getBoundingBox();
        boolean inFrustum = frustum.intersection.testAab((float) (bb.minX - frustum.camX), (float) (bb.minY - frustum.camY), (float) (bb.minZ - frustum.camZ),
                (float) (bb.maxX - frustum.camX), (float) (bb.maxY - frustum.camY), (float) (bb.maxZ - frustum.camZ));
        if (camera.entity() == Minecraft.getInstance().player) {
            //vista?
            if (particle instanceof CustomParticleInstance cpt) {
                cpt.notifyInFrustum(inFrustum);
            }
        }
        return inFrustum;
    }

}
