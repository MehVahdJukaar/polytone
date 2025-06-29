package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;

public interface RotationProvider {

    Codec<RotationProvider> CODEC = Codec.withAlternative(
            (Codec<RotationProvider>) (Object) Custom.CODEC,
            RotationMode.CODEC);


    boolean alwaysFacesCamera();

    void applyRotation(SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks);

    record Custom(ParticleContextExpression xRot,
                  ParticleContextExpression yRot,
                  ParticleContextExpression zRot) implements RotationProvider {

        public static final Codec<Custom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ParticleContextExpression.CODEC.optionalFieldOf("x_rot", ParticleContextExpression.ZERO).forGetter(Custom::xRot),
                ParticleContextExpression.CODEC.optionalFieldOf("y_rot", ParticleContextExpression.ZERO).forGetter(Custom::yRot),
                ParticleContextExpression.CODEC.optionalFieldOf("z_rot", ParticleContextExpression.ZERO).forGetter(Custom::zRot)
        ).apply(instance, Custom::new));

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
