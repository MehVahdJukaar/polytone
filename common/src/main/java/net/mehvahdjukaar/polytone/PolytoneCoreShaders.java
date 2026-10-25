package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;

//game reloads these along with the vanilla core shaders
public class PolytoneCoreShaders {

    public static ShaderInstance particleTranslucent;
    public static ShaderInstance depthCombine;

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                s -> particleTranslucent = s);
        PlatStuff.registerShaders(Polytone.res("depth_combine"), DefaultVertexFormat.POSITION_TEX,
                s -> depthCombine = s);
    }
}
