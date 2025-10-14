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
import org.lwjgl.opengl.GL13;

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


    public static final ParticleRenderType PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE = new ParticleRenderType() {

        @Override
        public BufferBuilder begin(Tesselator builder, TextureManager textureManager) {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            //because of custom render type fuckery...

            RenderSystem.setShader(() -> noAlphaCutoffShader);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
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
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
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
                    .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true)
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
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true)
    );


    public static void onRenderLast() {
        DEFERRED_BUFFER_SOURCE.endBatches();
    }

    public static final DeferredBufferSource DEFERRED_BUFFER_SOURCE = new DeferredBufferSource();

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
            if (delayed.contains(ADDITIVE_TRANSLUCENT_BLOCK)) {
                endBatch(ADDITIVE_TRANSLUCENT_BLOCK);
                delayed.remove(ADDITIVE_TRANSLUCENT_BLOCK);
            }
            if (delayed.contains(ADDITIVE_TRANSLUCENT_PARTICLE)) {
                endBatch(ADDITIVE_TRANSLUCENT_PARTICLE);
                delayed.remove(ADDITIVE_TRANSLUCENT_PARTICLE);
            }
            for (RenderType type : delayed) {
                endBatch(type);
            }
        }

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
            if (!fixedBuffers.containsKey(renderType)) {
                fixedBuffers.put(renderType, bufferSupplier.get());
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