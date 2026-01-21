package net.mehvahdjukaar.polytone.content.particle.custom;


import com.mojang.serialization.Codec;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;

public enum RotationMode implements StringRepresentable, IRotationProvider {
    LOOK_AT_XYZ, LOOK_AT_Y,
    LOOK_AT_X, LOOK_AT_Z, LOOK_AT_XZ,
    MOVEMENT_ALIGNED, LOOK_UP, LOOK_WEST, NONE;

    public static final Codec<RotationMode> CODEC = StringRepresentable.fromEnum(RotationMode::values);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void setRotation(@Nullable SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
        switch (this) {
            case NONE -> {
            }
            case LOOK_UP -> {
                quaternionf.identity(); // Reset rotation
                quaternionf.rotateX(Mth.HALF_PI);
            }
            case LOOK_WEST -> {
                quaternionf.identity(); // Reset rotation
                quaternionf.rotateY(Mth.HALF_PI);
            }
            case LOOK_AT_XYZ ->
                    SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(quaternionf, camera, partialTicks);
            case LOOK_AT_Y ->
                    SingleQuadParticle.FacingCameraMode.LOOKAT_Y.setRotation(quaternionf, camera, partialTicks);
            case LOOK_AT_X -> quaternionf.set(camera.rotation().x, 0, 0.0F, camera.rotation().w);
            case LOOK_AT_Z -> quaternionf.set(0.0F, 0.0F, camera.rotation().z, camera.rotation().w);
            case LOOK_AT_XZ -> quaternionf.set(camera.rotation().x, 0, camera.rotation().z, camera.rotation().w);
            case MOVEMENT_ALIGNED -> {
                Vec3 dir = particle == null ? Vec3.ZERO :
                        new Vec3(particle.xd, particle.yd, particle.zd).normalize();

                Vec3 cameraLook = new Vec3(camera.forwardVector());
                Vec3 cross = dir.cross(cameraLook);

                double pitch = getPitch(dir);
                double yaw = getYaw(dir);

                Vector3f dirUp = new Vector3f(0, 1, 0).rotate(quaternionf);
                float roll = dirUp.angleSigned(cross.toVector3f(), dir.toVector3f());

                quaternionf.rotateY((float) (-Mth.DEG_TO_RAD * yaw));

                quaternionf.rotateX((float) (Mth.DEG_TO_RAD * (pitch - 90)));
                quaternionf.rotateY(-roll - Mth.HALF_PI);
            }
        }
    }

    @Override
    public boolean alwaysFacesCamera() {
        return (this == LOOK_AT_XYZ || this == LOOK_AT_Y || this == MOVEMENT_ALIGNED);
    }

    // in degrees. Opposite of Vec3.fromRotation
    public static double getPitch(Vec3 vec3) {
        return -Math.toDegrees(Math.asin(vec3.y));
    }

    // in degrees
    public static double getYaw(Vec3 vec3) {
        return Math.toDegrees(Math.atan2(-vec3.x, vec3.z));
    }

}

