package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;

public interface RotationProvider {

    Codec<RotationProvider> CODEC = Codec.withAlternative(
            (Codec<RotationProvider>) (Object) CustomRotation.CODEC,
            RotationMode.CODEC);


    boolean alwaysFacesCamera();

    void applyRotation(SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks);

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
        public void applyRotation(SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
            var level = Minecraft.getInstance().level;
            double x = this.xRot.getValue(particle, level);
            double y = this.yRot.getValue(particle, level);
            double z = this.zRot.getValue(particle, level);

            quaternionf.rotateXYZ((float) x, (float) y, (float) z);
        }

    }
}
