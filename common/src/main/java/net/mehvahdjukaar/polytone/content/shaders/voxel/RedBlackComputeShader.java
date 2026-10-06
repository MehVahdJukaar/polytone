package net.mehvahdjukaar.polytone.content.shaders.voxel;

import com.mojang.blaze3d.platform.GlStateManager;
import net.mehvahdjukaar.polytone.content.shaders.ComputeProgramInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

//pairs with polytone_red_black.glsl
public abstract class RedBlackComputeShader extends ComputeProgramInstance {

    //4x4x4 groups. 64 workers each. that means 2 wraps each. maybe make 1 wrap each. maybe makke 1?
    private static final int GROUP_EDGE = 4;
    private static final int WORKERS_PER_GROUP = GROUP_EDGE * GROUP_EDGE * GROUP_EDGE;

    //we run RB algorithm. kept between calls so its fair.
    private boolean isRedPass = false;

    protected RedBlackComputeShader(ResourceLocation path) {
        super(path);
    }

    protected void setPlacement(Vector3i sizeInCells, Vector3i originTexel) {
        setUniform("Size", sizeInCells.x, sizeInCells.y, sizeInCells.z);
        setUniform("OriginTexel", originTexel.x, originTexel.y, originTexel.z);
    }

    //images and the other uniforms must be set already
    protected void dispatchIterations(int iterations, Vector3i boxSize) {
        //finagling current vanilla shader
        int previousProgram = GlStateManager._getInteger(GL20.GL_CURRENT_PROGRAM);
        GlStateManager._glUseProgram(programId());

        int redPassLocation = uniformLocation("RedPass");
        for (int i = 0; i < iterations; i++) {
            GlStateManager._glUniform1i(redPassLocation, isRedPass ? 1 : 0);
            //each step only does one checkerboard color, so half the threads along x
            GL43.glDispatchCompute(Mth.positiveCeilDiv(boxSize.x / 2, GROUP_EDGE),
                    Mth.positiveCeilDiv(boxSize.y, GROUP_EDGE), Mth.positiveCeilDiv(boxSize.z, GROUP_EDGE));

            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            isRedPass = !isRedPass;
        }

        //el barrier for texture fetch. needed since this light texture is read in the render pass right after
        GL42.glMemoryBarrier(GL42.GL_TEXTURE_FETCH_BARRIER_BIT);

        GlStateManager._glUseProgram(previousProgram);
    }
}
