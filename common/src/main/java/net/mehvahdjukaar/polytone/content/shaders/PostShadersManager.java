package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class PostShadersManager {

    public static final String GLOBALS_NAME = "PolyGlobals";
    private PolytoneGlobalUniforms globalUniforms = null;

    private PolytoneGlobalUniforms getOrCreateUniforms(){
        if(globalUniforms == null){
            globalUniforms = new PolytoneGlobalUniforms();
        }
        return globalUniforms;
    }


    public void setupExtraUniforms(RenderPass pass) {
        pass.setUniform(GLOBALS_NAME, getOrCreateUniforms().getSlice());
    }

    public void onClose() {
        if(globalUniforms != null) {
            globalUniforms.close();
            globalUniforms = null;
        }
    }

    public void onRenderLevel(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        float angle = Minecraft.getInstance().levelRenderer.levelRenderState.skyRenderState.sunAngle;
        getOrCreateUniforms().update(projectionMatrix, viewMatrix, angle);
    }

}
