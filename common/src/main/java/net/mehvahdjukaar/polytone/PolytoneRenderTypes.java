package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;

public class PolytoneRenderTypes extends RenderType {
    static ShaderInstance particleNoAlphaCutoffShader;

    public PolytoneRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                s -> particleNoAlphaCutoffShader = s);
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
            RenderSystem.setShader(() -> particleNoAlphaCutoffShader);

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);

        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public String toString() {
            return "POLYTONE_PARTICLE_SHEET_ADDITIVE_TRANSLUCENT";
        }
    };



    public static void onRenderLast() {
        if (lastModelViewMatrix != null) {
            Matrix4f last = new Matrix4f(RenderSystem.getModelViewMatrix());
            RenderSystem.getModelViewMatrix().set(lastModelViewMatrix);
            DEFERRED_BUFFER_SOURCE.endBatches();
            RenderSystem.getModelViewMatrix().set(last);
        }else {
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
            endBatch(ADDITIVE_TRANSLUCENT_BLOCK);
            endBatch(ADDITIVE_TRANSLUCENT_PARTICLE);
            for (RenderType type : delayed) {
                endBatch(type);
            }
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

};