package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL13;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;

public class PolytoneRenderTypes extends RenderType {

    static ShaderInstance instance;

    public PolytoneRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                s -> instance = s);
    }

    protected static final TransparencyStateShard ADDITIVE_TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard(
            "polytone_additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    public static final RenderType ADDITIVE_TRANSLUCENT =
            create("polytone_additive_translucent",
                    DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS,
                    786432, true, true,
                    RenderType.CompositeState.builder()
                            .setLightmapState(LIGHTMAP)
                            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                            .setTextureState(BLOCK_SHEET_MIPPED)
                            .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                            .setOutputState(TRANSLUCENT_TARGET).createCompositeState(true));


    public static final ParticleRenderType ADDITIVE_TRANSLUCENT_PARTICLE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            //because of custom render type fuckery...

            RenderSystem.setShader(() -> instance);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public String toString() {
            return "POLYTONE_PARTICLE_SHEET_ADDITIVE_TRANSLUCENT";
        }
    };


};
