package net.mehvahdjukaar.polytone.content.shaders.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneCoreShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;

public class LevelDepthSnapshot implements AutoCloseable {

    private TextureTarget target = null;
    private boolean holdsLevelDepth = false;

    public int textureId() {
        return target == null ? 0 : target.getDepthTextureId();
    }

    public void captureLevelDepth() {
        copyDepthFromMain();
        holdsLevelDepth = true;
    }

    //post chains run after the hand, which cleared and redrew depth
    public void prepareForPostChains() {
        if (!holdsLevelDepth) {
            copyDepthFromMain();
        } else if (Polytone.CONFIGS.postShadersOccludeHeldItems.get()) {
            applyItemDepth();
        }
        holdsLevelDepth = false;
    }

    private void copyDepthFromMain() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (target == null) {
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        //for neoforge
        PlatStuff.matchStencil(main, target);
        target.copyDepthFrom(main);
        main.bindWrite(false);
    }

    private void applyItemDepth() {
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = PolytoneCoreShaders.depthCombine;
        RenderTarget main = mc.getMainRenderTarget();

        target.bindWrite(true);

        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        shader.setSampler("InSampler", main.getDepthTextureId());
        RenderSystem.setShader(() -> shader);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        bb.addVertex(1f, -1f, 0f).setUv(1f, 0f);
        bb.addVertex(1f, 1f, 0f).setUv(1f, 1f);
        bb.addVertex(-1f, 1f, 0f).setUv(0f, 1f);
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        main.bindWrite(false);
    }

    public void resize(int width, int height) {
        if (target != null) target.resize(width, height, Minecraft.ON_OSX);
    }

    @Override
    public void close() {
        if (target == null) return;
        target.destroyBuffers();
        target = null;
    }
}
