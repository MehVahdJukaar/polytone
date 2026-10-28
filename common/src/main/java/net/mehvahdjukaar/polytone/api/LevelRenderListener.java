package net.mehvahdjukaar.polytone.api;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import org.joml.Matrix4fc;

public interface LevelRenderListener {

    //main camera only, before the frame graph is built. extra level renders are fine here
    default void beforeLevelRender(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, GpuBufferSlice terrainFog) {
    }

    //main camera only, after the frame graph ran
    default void afterLevelRender(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix) {
    }
}
