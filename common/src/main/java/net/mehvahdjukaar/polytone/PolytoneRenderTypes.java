package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;

public class PolytoneRenderTypes extends RenderType {

    private static ShaderInstance noAlphaCutoffShader;

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                s -> noAlphaCutoffShader = s);
    }


    //kind of not used since particle render type modify buffer replaces it
    public static final ParticleRenderType PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE = new ParticleRenderType() {

        @Override
        public BufferBuilder begin(Tesselator builder, TextureManager textureManager) {
            // Normally the modifyParticleConsumer redirect sends these vertices to DEFERRED_BUFFER_SOURCE,
            // leaving this buffer empty. However, mods that optimize particle rendering (e.g. Sodium) may
            // short-circuit SingleQuadParticle#renderRotatedQuad before our redirect runs, causing geometry
            // to land here instead. Set up the additive-translucent state so such a stray draw is correct
            // rather than rendered with leftover shader/blend state (which showed up as corrupted particles).
            RenderSystem.setShader(() -> noAlphaCutoffShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return builder.begin(VertexFormat.Mode.QUADS, PARTICLE);
        }

        @Override
        public String toString() {
            return "PARTICLE_SHEET_ADDITIVE_TRANSLUCENT";
        }
    };


    public PolytoneRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    protected static final TransparencyStateShard ADDITIVE_TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("polytone:additive_translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false); //needed for additive to avoid z fight

    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    });

    private static final TextureStateShard PARTICLE_SHEET = new TextureStateShard(TextureAtlas.LOCATION_PARTICLES, false, false);
    protected static final ShaderStateShard PARTICLE_SHADER_STATE = new ShaderStateShard(() -> noAlphaCutoffShader);

    public static final RenderType ADDITIVE_TRANSLUCENT_PARTICLE = RenderType.create(
            Polytone.MOD_ID + ":additive_translucent_particle",
            DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS,
            786432,
            true, true,
            RenderType.CompositeState.builder()
                    .setLightmapState(LIGHTMAP)
                    .setTextureState(PARTICLE_SHEET)
                    .setShaderState(PARTICLE_SHADER_STATE)
                    .setWriteMaskState(WriteMaskStateShard.COLOR_WRITE)
                    .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                    // .setOutputState(CLOUDS_TARGET) //for fabulous. it will draw this onto its own target
                    .createCompositeState(false)
    );

    public static final RenderType ADDITIVE_TRANSLUCENT_BLOCK = RenderType.create(
            Polytone.MOD_ID + ":additive_translucent_block",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true, true,
            RenderType.CompositeState.builder()
                    .setLightmapState(LIGHTMAP)
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                    //  .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true)
    );


    public static void onRenderLast() {
        if (lastModelViewMatrix != null) {
            Matrix4f last = new Matrix4f(RenderSystem.getModelViewMatrix());
            RenderSystem.getModelViewMatrix().set(lastModelViewMatrix);
            DEFERRED_BUFFER_SOURCE.endBatches();
            RenderSystem.getModelViewMatrix().set(last);
        } else {
            DEFERRED_BUFFER_SOURCE.endBatches();
        }
    }

    public static final DeferredBufferSource DEFERRED_BUFFER_SOURCE = new DeferredBufferSource();

    private static Matrix4f lastModelViewMatrix;

    public static void cacheMatrices() {
        lastModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
    }

    public static class DeferredBufferSource extends MultiBufferSource.BufferSource {
        protected final Supplier<ByteBufferBuilder> bufferSupplier;

        private final Collection<RenderType> delayed = new HashSet<>();

        protected DeferredBufferSource() {
            this(() -> new ByteBufferBuilder(786432), new LinkedHashMap<>());
        }

        protected DeferredBufferSource(Supplier<ByteBufferBuilder> bufferSupplier, SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
            super(bufferSupplier.get(), fixedBuffers);
            this.bufferSupplier = bufferSupplier;
        }

        public void endBatches() {
            //just making sure
            int currentFB = GlStateManager.getBoundFramebuffer();
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            endBatch(ADDITIVE_TRANSLUCENT_BLOCK);
            endBatch(ADDITIVE_TRANSLUCENT_PARTICLE);
            for (RenderType type : delayed) {
                endBatch(type);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, currentFB);
        }

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
            if (!fixedBuffers.containsKey(renderType)) {
                fixedBuffers.put(renderType, bufferSupplier.get());
                if (renderType != ADDITIVE_TRANSLUCENT_BLOCK && renderType != ADDITIVE_TRANSLUCENT_PARTICLE)
                    delayed.add(renderType);
            }
            return super.getBuffer(renderType);
        }

        @Override
        public void endBatch(@NotNull RenderType renderType) {
            super.endBatch(renderType);
        }
    }

}