package net.mehvahdjukaar.polytone.content.viewpoint;

import net.minecraft.client.Camera;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// viewpoint rotation at the main camera's position, for extracting particles
public class ViewpointCamera extends Camera {

    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f forwards = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Vector3f left = new Vector3f();

    public void setup(Camera mainCamera, Matrix4fc view, float xRot, float yRot) {
        this.setEntity(mainCamera.entity()); // look_at_player stays on the player
        this.setPosition(mainCamera.position());
        this.setRotation(yRot, xRot); // keeps xRot()/yRot() and the cached matrices in step
        // off the view itself, so z_rot and the straight up/down basis match what's rendered
        view.getNormalizedRotation(this.rotation).conjugate();
        this.rotation.transform(0, 0, -1, this.forwards);
        this.rotation.transform(0, 1, 0, this.up);
        this.rotation.transform(-1, 0, 0, this.left);
    }

    @Override
    public Quaternionf rotation() {
        return this.rotation;
    }

    @Override
    public Vector3fc forwardVector() {
        return this.forwards;
    }

    @Override
    public Vector3fc upVector() {
        return this.up;
    }

    @Override
    public Vector3fc leftVector() {
        return this.left;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }
}
