package net.mehvahdjukaar.polytone.api;

import net.minecraft.client.Camera;
import org.joml.Matrix4fc;

@FunctionalInterface
public interface LevelRenderListener {

    //main camera only, after the frame graph ran
    void afterLevelRender(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix);
}
