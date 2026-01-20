package net.mehvahdjukaar.polytone.content.particle.custom.render;

import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.world.phys.AABB;

public class ModelParticleRenderGroup extends ParticleGroup<CustomParticleType.Instance> {

    final ModelParticleRenderState particleTypeRenderState = new ModelParticleRenderState();

    public ModelParticleRenderGroup(ParticleEngine particleEngine) {
        super(particleEngine);
    }


    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float f) {
        for (CustomParticleType.Instance particle : this.particles) {
            if (particleInFrustum(frustum, particle)) {
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

        return this.particleTypeRenderState;
    }


    public static boolean particleInFrustum(Frustum frustum, Particle particle) {
        AABB bb = particle.getBoundingBox();
        return frustum.intersection.testAab((float) (bb.minX - frustum.camX), (float) (bb.minY - frustum.camY), (float) (bb.minZ - frustum.camZ),
                (float) (bb.maxX - frustum.camX), (float) (bb.maxY - frustum.camY), (float) (bb.maxZ - frustum.camZ));
    }

}
