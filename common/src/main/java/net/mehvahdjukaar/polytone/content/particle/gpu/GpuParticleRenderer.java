package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.content.particle.custom.RotationMode;
import net.mehvahdjukaar.polytone.content.shaders.ExpressionUniformBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public final class GpuParticleRenderer implements ParticleProvider<ParticleOptions>, AutoCloseable {

    // vanilla ADDITIVE is (ONE, ONE); particles want the source alpha to fade the add out
    private static final BlendFunction ADDITIVE_PARTICLE_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);

    private static final int INFO_UBO_SIZE = new Std140SizeCalculator()
            .putVec3().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putInt()
            .putVec2().putInt().putInt()
            .putInt().putVec4()
            .get();

    private final Identifier id;
    private final String debugLabel;
    private final GpuParticleType type;
    private final GpuParticleBuffer records;
    private final ExpressionUniformBuffers customUniforms;
    private final RandomSource random = RandomSource.create();
    private @Nullable RenderPipeline pipeline;
    private @Nullable GpuBuffer infoUbo;
    private boolean shaderFailed = false;
    // resolved in prepare: both of these can upload or grow GPU storage, which a render pass forbids
    private @Nullable GpuTextureView texture;
    private @Nullable GpuBuffer indexBuffer;
    private VertexFormat.IndexType indexType = VertexFormat.IndexType.SHORT;
    private int indexCount;
    private int debugFrames = 0;

    public GpuParticleRenderer(Identifier id, GpuParticleType type) {
        this.id = id;
        this.debugLabel = "Polytone gpu particle " + id;
        this.type = type;
        this.records = new GpuParticleBuffer(type.limit());
        this.customUniforms = new ExpressionUniformBuffers(type.uniforms());
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
        int light = LevelRenderer.getLightColor(level, pos);
        if (type.lightLevel() > 0) {
            light = LightTexture.pack(Math.max(LightTexture.block(light), type.lightLevel()), LightTexture.sky(light));
        }
        records.add(x, y, z, (float) dx, (float) dy, (float) dz, level.getGameTime(), random.nextFloat(), light, values);
        return null;
    }

    // null once the shader failed to compile: drawing with a broken program is a GL error storm
    private @Nullable RenderPipeline pipeline() {
        if (shaderFailed) return null;
        if (pipeline == null) {
            Identifier shader = Identifier.fromNamespaceAndPath(type.shader().getNamespace(), "core/" + type.shader().getPath());
            RenderPipeline.Builder builder = RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Polytone.res("pipeline/gpu_particle/" + id.getNamespace() + "/" + id.getPath()))
                    .withVertexShader(shader)
                    .withFragmentShader(shader)
                    .withSampler("Sampler0")
                    .withSampler("Sampler2")
                    .withUniform("ParticleInfo", UniformType.UNIFORM_BUFFER)
                    .withVertexFormat(GpuParticleBuffer.FORMAT, VertexFormat.Mode.QUADS)
                    .withCull(false);
            for (String name : type.uniforms().keySet()) {
                builder.withUniform(name, UniformType.UNIFORM_BUFFER);
            }
            switch (type.renderType()) {
                case TRANSLUCENT -> builder.withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false);
                case ADDITIVE_TRANSLUCENT -> builder.withBlend(ADDITIVE_PARTICLE_BLEND).withDepthWrite(false);
                default -> builder.withoutBlend().withDepthWrite(true);
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

    // no render pass may be open here: everything that writes to a buffer happens in this half
    public boolean prepare(Vec3 cameraPos, long gameTime, float partialTick) {
        if (type.renderType() == ParticleRenderMode.INVISIBLE) return false;
        if (pipeline() == null) return false;
        records.prepareForFrame(cameraPos, gameTime);
        customUniforms.ensureInitialized(debugLabel);
        customUniforms.update();
        if (infoUbo == null) {
            infoUbo = RenderSystem.getDevice().createBuffer(() -> debugLabel + " info",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, INFO_UBO_SIZE);
        }
        writeInfo(cameraPos, gameTime, partialTick);

        // getTexture loads and uploads the image the first time it is asked for
        texture = Minecraft.getInstance().getTextureManager().getTexture(type.texture()).getTextureView();
        indexCount = records.vertexCount() / 4 * 6;
        RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        indexBuffer = sequential.getBuffer(indexCount);
        indexType = sequential.type();
        if (Polytone.isDevEnv && debugFrames++ % 200 == 0) {
            Polytone.LOGGER.info("[gpu debug] {} drawing {} indices, Time {}, origin {}, texture {}",
                    id, indexCount, (gameTime - records.timeBase()) + partialTick, records.origin(), texture);
        }
        return true;
    }

    private void writeInfo(Vec3 cameraPos, long gameTime, float partialTick) {
        Vec3 origin = records.origin();
        int colorEnd = type.colorEnd().orElse(-1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, INFO_UBO_SIZE)
                    .putVec3((float) (origin.x - cameraPos.x), (float) (origin.y - cameraPos.y), (float) (origin.z - cameraPos.z))
                    .putFloat((gameTime - records.timeBase()) + partialTick)
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
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(infoUbo.slice(), bb);
        }
    }

    public void draw(RenderPass pass) {
        GpuBuffer vertices = records.vertexBuffer();
        if (pipeline == null || vertices == null || texture == null || indexBuffer == null) return;

        pass.setPipeline(pipeline);
        pass.bindTexture("Sampler0", texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        pass.setUniform("ParticleInfo", infoUbo);
        customUniforms.bind(pass, type.uniforms().keySet());
        pass.setVertexBuffer(0, vertices);
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.drawIndexed(0, 0, indexCount, 1);
    }

    private boolean isTranslucent() {
        return type.renderType() == ParticleRenderMode.TRANSLUCENT
                || type.renderType() == ParticleRenderMode.ADDITIVE_TRANSLUCENT;
    }

    // per-tick friction f means v *= f each tick, i.e. dv/dt = -k v with k = -ln f
    private static float dragOf(float friction) {
        return friction >= 1f ? 0f : (float) -Math.log(Math.max(friction, 1e-4));
    }

    // the shader knows four orientations, the other RotationMode values fall back to the camera one
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
        records.close();
        customUniforms.close();
        if (infoUbo != null) infoUbo.close();
        infoUbo = null;
        pipeline = null;
    }
}
