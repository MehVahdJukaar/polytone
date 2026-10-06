package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.Polytone;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL42;

public class FarLightSpreadComputeShader extends RedBlackComputeShader {

    public FarLightSpreadComputeShader() {
        super(Polytone.res("shaders/compute/far_light_spread.csh"));
    }

    //always the whole volume, its tiny
    public void runCompute(FarLightGrid grid, int iterations) {
        setPlacement(grid.sizeInCells(), grid.originTexel());

        GL42.glBindImageTexture(0, grid.emissionTextureId(), 0, true, 0, GL15.GL_READ_ONLY, GL11.GL_RGBA8);
        GL42.glBindImageTexture(1, grid.lightTextureId(), 0, true, 0, GL15.GL_READ_WRITE, GL11.GL_RGB10_A2);

        dispatchIterations(iterations, grid.sizeInCells());
    }
}
