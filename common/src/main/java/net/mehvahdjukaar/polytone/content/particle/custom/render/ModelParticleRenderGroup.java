package net.mehvahdjukaar.polytone.content.particle.custom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class ModelParticleRenderGroup extends ParticleGroup<CustomParticleType.Instance> {

    private int particleCount;
    private final ParticleRenderType particleType;
    final ModelParticleRenderState particleTypeRenderState = new ModelParticleRenderState();

    public ModelParticleRenderGroup(ParticleEngine particleEngine, ParticleRenderType particleRenderType) {
        super(particleEngine);
        this.particleType = particleRenderType;
    }


    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float f) {
        for (CustomParticleType.Instance particle : this.particles) {
            if (frustum.pointInFrustum(particle.x, particle.y, particle.z)) {
                try {
                    particle.extractModel(this.particleTypeRenderState, camera, f);
                } catch (Throwable var9) {
                    CrashReport crashReport = CrashReport.forThrowable(var9, "Rendering Particle");
                    CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
                    crashReportCategory.setDetail("Particle", particle::toString);
                    crashReportCategory.setDetail("Particle Type", this.particleType::toString);
                    throw new ReportedException(crashReport);
                }
            }
        }

        return this.particleTypeRenderState;
    }


}
