package net.mehvahdjukaar.polytone.content.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.content.particle.ParticleContextExpression;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public record ItemModelParticleEmitter(ParticleContextExpression exp) {

    /**
     * Converts the top of the PoseStack (in view space) into world-space by applying the camera's transform.
     *
     * @param poseStack The current PoseStack, in view space.
     * @return A Matrix4f representing the model's transform in world space.
     */
    public static Matrix4f getWorldMatrixFromModelMatrix(PoseStack poseStack) {
        Matrix4f modelView = new Matrix4f(poseStack.last().pose());

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        Vec3 camPos = camera.getPosition();
        Quaternionf camRot = new Quaternionf(camera.rotation()).conjugate(); // invert the view rotation

        // Construct world-space camera matrix: apply rotation first, then translation
        Matrix4f cameraMatrix = new Matrix4f()
                .rotate(camRot)
                .translate((float) camPos.x, (float) camPos.y, (float) camPos.z);

        // Apply the inverse view transform to get world space
        return cameraMatrix.mul(modelView);
    }
}
