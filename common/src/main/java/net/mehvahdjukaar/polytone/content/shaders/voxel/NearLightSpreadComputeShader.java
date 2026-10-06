package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.Polytone;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;

//first compute shader, here we go.
public class NearLightSpreadComputeShader extends RedBlackComputeShader {

    public NearLightSpreadComputeShader() {
        super(Polytone.res("shaders/compute/near_light_spread.csh"));
    }

    //only the box from minCell, sized in blocks. multiples of 16 so they fit the groups
    public void runCompute(NearLight light, CellPalette palette, int iterations, Vector3i minCell, Vector3i boxSize) {
        PaletteGrid grid = light.grid();
        setPlacement(grid.sizeInCells(), grid.originTexel());
        setUniform("BoxStart", minCell.x, minCell.y, minCell.z);

        GL42.glBindImageTexture(0, grid.textureId(), 0, true, 0, GL15.GL_READ_ONLY, GL30.GL_R16UI);
        GL42.glBindImageTexture(1, palette.textureId(), 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RGBA32UI);

        GL42.glBindImageTexture(2, light.lightTextureId(), 0, true, 0, GL15.GL_READ_WRITE, GL11.GL_RGB10_A2);

        dispatchIterations(iterations, boxSize);
    }

}
