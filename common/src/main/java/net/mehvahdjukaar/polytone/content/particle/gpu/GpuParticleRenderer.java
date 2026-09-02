package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.ICustomParticleFactory;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.content.particle.custom.RotationMode;
import net.mehvahdjukaar.polytone.content.shaders.ExpressionUniformBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

// most nonsense system in polytone
public final class GpuParticleRenderer implements ICustomParticleFactory, AutoCloseable {

    public static final Codec<GpuParticleRenderer> CODEC = GpuParticleType.CODEC
            .xmap(GpuParticleRenderer::new, GpuParticleRenderer::type);

    private static final BlendFunction ADDITIVE_PARTICLE_BLEND = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);

    private static final int INFO_UBO_SIZE = new Std140SizeCalculator()
            .putVec4()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putInt()
            .putVec2().putInt().putInt()
            .putInt().putVec4()
            .putVec4().putVec4()
            .putFloat().putInt().putInt().putFloat()
            .get();

    private Identifier id = Polytone.res("unnamed");
    private String debugLabel = "Polytone gpu particle";
    private final GpuParticleType type;
    private final GpuParticleBuffer records;
    private final ExpressionUniformBuffers customUniforms;
    private final RandomSource random = RandomSource.create();
    private @Nullable RenderPipeline pipeline;
    private @Nullable GpuBuffer infoUbo;
    private boolean shaderFailed = false;
    private boolean closed = false;
    private @Nullable GpuTextureView texture;
    private @Nullable GpuParticleHeightmap heightmap;

    public GpuParticleRenderer(GpuParticleType type) {
        this.type = type;
        this.records = new GpuParticleBuffer(type.limit(), type.quadsPerSpawn());
        this.customUniforms = new ExpressionUniformBuffers(type.uniforms());
    }

    public GpuParticleType type() {
        return type;
    }

    public void setId(Identifier id) {
        this.id = id;
        this.debugLabel = "Polytone gpu particle " + id;
    }

    @Override
    public void setSpriteSet(SpriteSet spriteSet) {
    }

    @Override
    public boolean isValid() {
        return !closed;
    }

    @Override
    public boolean forceSpawns() {
        return false;
    }

    @Override
    public @Nullable Particle createParticle(ParticleOptions options, ClientLevel level,
                                             double x, double y, double z, double dx, double dy, double dz,
                                             RandomSource rand) {
        BlockPos pos = BlockPos.containing(x, y, z);
        GpuParticleInitializer.SpawnValues values = type.initializer().evaluate(level, new Vec3(x, y, z), level.getBlockState(pos));
        if (options instanceof ExtraDataParticleOptions extra) {
            extra.extraData().forEach(values::override);
        }
        int light = LightCoordsUtil.getLightCoords(level, pos);
        if (type.lightLevel() > 0) {
            light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(light), type.lightLevel()), LightCoordsUtil.sky(light));
        }
        records.add(x, y, z, (float) dx, (float) dy, (float) dz, level.getGameTime(), random.nextFloat(), light, values);
        return null;
    }

    private @Nullable RenderPipeline pipeline() {
        if (shaderFailed) return null;
        if (pipeline == null) {
            Identifier shader = Identifier.fromNamespaceAndPath(type.shader().getNamespace(), "core/" + type.shader().getPath());
            // 26.2 declares samplers and uniform blocks through bind group layouts instead of on the pipeline
            BindGroupLayout.Builder ownBlocks = BindGroupLayout.builder()
                    .withUniform("ParticleInfo", UniformType.UNIFORM_BUFFER);
            for (String name : type.uniforms().keySet()) {
                ownBlocks.withUniform(name, UniformType.UNIFORM_BUFFER);
            }
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Polytone.res("pipeline/gpu_particle/" + id.getNamespace() + "/" + id.getPath()))
                    .withVertexShader(shader)
                    .withFragmentShader(shader)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
                    .withBindGroupLayout(ownBlocks.build())
                    .withVertexBinding(0, GpuParticleBuffer.FORMAT)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withCull(false);
            switch (type.renderType()) {
                case TRANSLUCENT -> builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false));
                case ADDITIVE_TRANSLUCENT -> builder.withColorTargetState(new ColorTargetState(ADDITIVE_PARTICLE_BLEND))
                        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false));
                default -> builder.withColorTargetState(ColorTargetState.DEFAULT)
                        .withDepthStencilState(DepthStencilState.DEFAULT);
            }
            pipeline = builder.build();
            if (!RenderSystem.getDevice().precompilePipeline(pipeline, null).isValid()) {
                Polytone.LOGGER.error("Failed to compile shader {} for gpu particle {}", type.shader(), id);
                shaderFailed = true;
                pipeline = null;
                return null;
            }
        }
        return pipeline;
    }

    public boolean prepare(Vec3 cameraPos, long gameTime, float partialTick, GpuParticleHeightmap heightmap) {
        if (type.renderType() == ParticleRenderMode.INVISIBLE) return false;
        if (pipeline() == null) return false;
        records.prepareForFrame(cameraPos, gameTime);
        customUniforms.ensureInitialized(debugLabel);
        customUniforms.update();
        if (infoUbo == null) {
            infoUbo = RenderSystem.getDevice().createBuffer(() -> debugLabel + " info",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, INFO_UBO_SIZE);
        }
        writeInfo(cameraPos, gameTime, partialTick, heightmap);
        this.heightmap = heightmap;

        texture = Minecraft.getInstance().getTextureManager().getTexture(type.texture()).getTextureView();
        return true;
    }

    private void writeInfo(Vec3 cameraPos, long gameTime, float partialTick, GpuParticleHeightmap heightmap) {
        Vec3 origin = records.origin();
        int colorEnd = type.colorEnd().orElse(-1);
        Vec3 areaSize = type.area().map(GpuParticleType.Area::size).orElse(Vec3.ZERO);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, INFO_UBO_SIZE)
                    .putVec4((float) (origin.x - cameraPos.x), (float) (origin.y - cameraPos.y),
                            (float) (origin.z - cameraPos.z), (gameTime - records.timeBase()) + partialTick)
                    .putFloat(type.gravity())
                    .putFloat(dragOf(type.friction()))
                    .putFloat(type.sway())
                    .putFloat(type.spin())
                    .putFloat(type.sizeEnd().orElse(-1f))
                    .putFloat(type.aspect())
                    .putFloat(isTranslucent() ? 0.003f : 0.1f)
                    .putInt(type.frames())
                    .putVec2(type.fade().in(), type.fade().out())
                    .putInt(billboardOf(type.rotationMode()))
                    .putInt(type.randomSprite() ? 1 : 0)
                    .putInt(type.colorEnd().isPresent() ? 1 : 0)
                    .putVec4(ARGB.red(colorEnd) / 255f, ARGB.green(colorEnd) / 255f,
                            ARGB.blue(colorEnd) / 255f, ARGB.alpha(colorEnd) / 255f)
                    .putVec4((float) (heightmap.originX() - cameraPos.x), (float) (heightmap.originZ() - cameraPos.z),
                            GpuParticleHeightmap.SIZE, heightmap.minY())
                    .putVec4((float) areaSize.x, (float) areaSize.y, (float) areaSize.z, 0f)
                    .putFloat((float) cameraPos.y)
                    .putInt(type.killBelowHeightmap() ? 1 : 0)
                    .putInt(type.quadsPerSpawn())
                    .putFloat(0f)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(infoUbo.slice(), bb);
        }
    }

    public int indexCount() {
        return records.vertexCount() / 4 * 6;
    }

    public void draw(RenderPass pass, GpuBuffer indexBuffer, IndexType indexType) {
        GpuBuffer vertices = records.vertexBuffer();
        if (pipeline == null || vertices == null || texture == null || heightmap == null) return;

        pass.setPipeline(pipeline);
        pass.bindTexture("Sampler0", texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        pass.bindTexture("Sampler1", heightmap.textureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        pass.setUniform("ParticleInfo", infoUbo);
        customUniforms.bind(pass, type.uniforms().keySet());
        pass.setVertexBuffer(0, vertices.slice());
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.drawIndexed(indexCount(), 1, 0, 0, 0);
    }

    private boolean isTranslucent() {
        return type.renderType() == ParticleRenderMode.TRANSLUCENT
                || type.renderType() == ParticleRenderMode.ADDITIVE_TRANSLUCENT;
    }

    private static float dragOf(float friction) {
        return friction >= 1f ? 0f : (float) -Math.log(Math.max(friction, 1e-4));
    }

    private static int billboardOf(RotationMode mode) {
        return switch (mode) {
            case LOOK_AT_Y, LOOK_AT_PLAYER_Y -> 1;
            case LOOK_UP -> 2;
            case MOVEMENT_ALIGNED -> 3;
            default -> 0;
        };
    }

    @Override
    public void close() {
        closed = true;
        records.close();
        customUniforms.close();
        if (infoUbo != null) infoUbo.close();
        infoUbo = null;
        pipeline = null;
    }
}
