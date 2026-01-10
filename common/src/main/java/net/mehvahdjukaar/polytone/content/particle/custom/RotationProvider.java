package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

//Extended facing camera mode
public interface RotationProvider extends SingleQuadParticle.FacingCameraMode {

    Codec<RotationProvider> CODEC = CodecUtils.alternatives(
            CustomRotation.CODEC, RotationMode.CODEC);


    boolean alwaysFacesCamera();

    void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks);

    @Override
    default void setRotation(Quaternionf quaternionf, Camera camera, float f) {
        setRotation(null, quaternionf, camera, f);
    }

    record CustomRotation(IParticleExp xRot,
                          IParticleExp yRot,
                          IParticleExp zRot) implements RotationProvider {

        public static final Codec<CustomRotation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IParticleExp.CODEC.optionalFieldOf("x_rot", IParticleExp.ZERO).forGetter(CustomRotation::xRot),
                IParticleExp.CODEC.optionalFieldOf("y_rot", IParticleExp.ZERO).forGetter(CustomRotation::yRot),
                IParticleExp.CODEC.optionalFieldOf("z_rot", IParticleExp.ZERO).forGetter(CustomRotation::zRot)
        ).apply(instance, CustomRotation::new));

        @Override
        public boolean alwaysFacesCamera() {
            return false;
        }

        @Override
        public void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
            if (particle == null) return;
            var level = Minecraft.getInstance().level;
            double x = this.xRot.evaluate(particle, level);
            double y = this.yRot.evaluate(particle, level);
            double z = this.zRot.evaluate(particle, level);

            quaternionf.rotateXYZ((float) x, (float) y, (float) z);
        }

    }
}
