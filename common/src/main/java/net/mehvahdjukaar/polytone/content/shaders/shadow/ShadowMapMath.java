package net.mehvahdjukaar.polytone.content.shaders.shadow;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShadowMapMath {

    //same as vanilla's sun placement in renderSky
    public static void directionTowardSunLight(ClientLevel level, float partialTick, Vector3f dest) {
        float sunAngle = level.getSunAngle(partialTick);
        dest.set(-Mth.sin(sunAngle), Mth.cos(sunAngle), 0f);
        if (dest.y < 0f) dest.negate();
    }

    public static void lightViewLookingAlong(Vector3f towardLight, Matrix4f dest) {
        //any up vector does, as long as it isn't parallel to the light
        boolean lightNearlyVertical = Math.abs(towardLight.y) > 0.99f;
        dest.setLookAlong(-towardLight.x, -towardLight.y, -towardLight.z,
                0f, lightNearlyVertical ? 0f : 1f, lightNearlyVertical ? 1f : 0f);
    }

    //texel snap, anchored to the camera's chunk corner; known accepted artifact, see research/POST_SHADOW_NOTES.md
    public static void snapProjectionToTexelGrid(Matrix4f lightProj, Matrix4f lightView, Vec3 camPos, float coverage, int resolution) {
        double anchorX = Mth.positiveModulo(camPos.x, 16.0);
        double anchorY = Mth.positiveModulo(camPos.y, 16.0);
        double anchorZ = Mth.positiveModulo(camPos.z, 16.0);
        double lightSpaceX = lightView.m00() * anchorX + lightView.m10() * anchorY + lightView.m20() * anchorZ;
        double lightSpaceY = lightView.m01() * anchorX + lightView.m11() * anchorY + lightView.m21() * anchorZ;
        double texelSize = 2.0 * coverage / resolution;
        lightProj.m30(lightProj.m30() + (float) (offsetToTexelGrid(lightSpaceX, texelSize) / coverage));
        lightProj.m31(lightProj.m31() + (float) (offsetToTexelGrid(lightSpaceY, texelSize) / coverage));
    }

    private static double offsetToTexelGrid(double lightSpaceCoord, double texelSize) {
        return lightSpaceCoord - Math.round(lightSpaceCoord / texelSize) * texelSize;
    }
}
