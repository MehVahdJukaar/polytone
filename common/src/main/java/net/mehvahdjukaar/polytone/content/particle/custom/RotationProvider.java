package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.particle.ParticleContextExpression;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

//Extended facing camera mode
public interface RotationProvider extends SingleQuadParticle.FacingCameraMode {

    Codec<RotationProvider> CODEC = Codec.withAlternative(
            (Codec<RotationProvider>) (Object) CustomRotation.CODEC,
            RotationMode.CODEC);


    boolean alwaysFacesCamera();

    void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks);

    @Override
    default void setRotation(Quaternionf quaternionf, Camera camera, float f) {
        setRotation(null, quaternionf, camera, f);
    }

    record CustomRotation(ParticleContextExpression xRot,
                          ParticleContextExpression yRot,
                          ParticleContextExpression zRot) implements RotationProvider {

        public static final Codec<CustomRotation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ParticleContextExpression.CODEC.optionalFieldOf("x_rot", ParticleContextExpression.ZERO).forGetter(CustomRotation::xRot),
                ParticleContextExpression.CODEC.optionalFieldOf("y_rot", ParticleContextExpression.ZERO).forGetter(CustomRotation::yRot),
                ParticleContextExpression.CODEC.optionalFieldOf("z_rot", ParticleContextExpression.ZERO).forGetter(CustomRotation::zRot)
        ).apply(instance, CustomRotation::new));

        @Override
        public boolean alwaysFacesCamera() {
            return false;
        }

        @Override
        public void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
            if (particle == null) return;
            var level = Minecraft.getInstance().level;
            double x = this.xRot.getValue(particle, level);
            double y = this.yRot.getValue(particle, level);
            double z = this.zRot.getValue(particle, level);

            quaternionf.rotateXYZ((float) x, (float) y, (float) z);
        }

    }
}
