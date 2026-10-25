package net.mehvahdjukaar.polytone.api;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;

@FunctionalInterface
public interface LevelRenderListener {

    //main camera only, after the level and before the hand
    void afterLevelRender(Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix);
}
