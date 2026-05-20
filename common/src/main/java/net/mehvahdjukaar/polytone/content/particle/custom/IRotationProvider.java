package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

//Extended facing camera mode
public interface IRotationProvider extends SingleQuadParticle.FacingCameraMode {

    Codec<IRotationProvider> CODEC = CodecUtils.alternatives(
            CustomRotation.CODEC, CustomFacingRotation.CODEC, RotationMode.CODEC);


    boolean alwaysFacesCamera();

    void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks);

    @Override
    default void setRotation(Quaternionf quaternionf, Camera camera, float f) {
        setRotation(null, quaternionf, camera, f);
    }

    record CustomRotation(IParticleExp xRot,
                          IParticleExp yRot,
                          IParticleExp zRot) implements IRotationProvider {

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


    record CustomFacingRotation(IParticleExp x,
                                IParticleExp y,
                                IParticleExp z) implements IRotationProvider {

        public static final Codec<CustomRotation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IParticleExp.CODEC.optionalFieldOf("x_forward", IParticleExp.ZERO).forGetter(CustomRotation::xRot),
                IParticleExp.CODEC.optionalFieldOf("y_forward", IParticleExp.ZERO).forGetter(CustomRotation::yRot),
                IParticleExp.CODEC.optionalFieldOf("z_forward", IParticleExp.ZERO).forGetter(CustomRotation::zRot)
        ).apply(instance, CustomRotation::new));

        @Override
        public boolean alwaysFacesCamera() {
            return true;
        }

        @Override
        public void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
            if (particle == null) return;
            var level = Minecraft.getInstance().level;
            double x = this.x.evaluate(particle, level);
            double y = this.y.evaluate(particle, level);
            double z = this.z.evaluate(particle, level);

            orientOverDirection(quaternionf, camera, new Vector3f((float) x, (float) y, (float) z));
        }

    }


    static void orientOverDirection(Quaternionf quaternionf, Camera camera, Vector3f dir) {
        var cameraLook = camera.forwardVector();
        Vector3f cross = dir.cross(cameraLook, new Vector3f());

        float pitch = getPitch(dir);
        float yaw = getYaw(dir);

        Vector3f dirUp = new Vector3f(0, 1, 0).rotate(quaternionf);
        float roll = dirUp.angleSigned(cross, dir);

        quaternionf.rotateY((-Mth.DEG_TO_RAD * yaw));

        quaternionf.rotateX((Mth.DEG_TO_RAD * (pitch - 90)));
        quaternionf.rotateY(-roll - Mth.HALF_PI);
    }

    // in degrees. Opposite of Vec3.fromRotation
    static float getPitch(Vector3f vec3) {
        return (float) (-Mth.RAD_TO_DEG * (Math.asin(vec3.y)));
    }

    // in degrees
    static float getYaw(Vector3f vec3) {
        return (float) (Mth.RAD_TO_DEG * (Mth.atan2(-vec3.x, vec3.z)));
    }

}
