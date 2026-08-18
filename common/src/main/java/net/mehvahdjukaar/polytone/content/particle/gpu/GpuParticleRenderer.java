package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.content.particle.custom.RotationMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

// One GPU particle type at runtime, both sides of it: the ParticleProvider that turns a spawn into a
// record (and returns no Particle), and the draw of all records with one program. The vertex buffer
// is a static dummy of limit quads; the shader reads everything from the record buffer via gl_VertexID.
public final class GpuParticleRenderer implements ParticleProvider<ParticleOptions>, AutoCloseable {

    // above the units ShaderInstance hands to the json samplers
    private static final int RECORD_SAMPLER_UNIT = 4;

    private final ResourceLocation id;
    private final GpuParticleType type;
    private final GpuParticleBuffer records;
    private final RandomSource random = RandomSource.create();
    private @Nullable ShaderInstance shader;
    private @Nullable VertexBuffer quads;
    private int recordSamplerLocation = -1;
    private boolean shaderFailed = false;

    public GpuParticleRenderer(ResourceLocation id, GpuParticleType type) {
        this.id = id;
        this.type = type;
        this.records = new GpuParticleBuffer(type.limit());
    }

    @Override
    public @Nullable Particle createParticle(ParticleOptions options, ClientLevel level,
                                             double x, double y, double z, double dx, double dy, double dz) {
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

    private boolean ensureLoaded(Minecraft mc) {
        if (shaderFailed) return false;
        if (shader == null) {
            try {
                shader = PlatStuff.createShader(mc.getResourceManager(), type.shader(), DefaultVertexFormat.POSITION);
                recordSamplerLocation = GL20.glGetUniformLocation(shader.getId(), "ParticleData");
            } catch (Exception e) {
                shaderFailed = true;
                Polytone.LOGGER.error("Failed to load shader {} for gpu particle {}", type.shader(), id, e);
                return false;
            }
        }
        if (quads == null) quads = buildDummyQuads(type.limit());
        return true;
    }

    private static VertexBuffer buildDummyQuads(int count) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        int vertices = count * 4;
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(vertices * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            for (int i = 0; i < vertices; i++) builder.addVertex(0, 0, 0);
            MeshData mesh = builder.buildOrThrow();
            buffer.bind();
            buffer.upload(mesh);
            VertexBuffer.unbind();
        }
        return buffer;
    }

    // modelView/projection are the level ones, so vertices come out camera-relative
    public void render(Minecraft mc, Vec3 cameraPos, long gameTime, float partialTick,
                       Matrix4f modelView, Matrix4f projection) {
        if (type.renderType() == ParticleRenderMode.INVISIBLE) return;
        if (!ensureLoaded(mc)) return;
        ShaderInstance shader = this.shader;
        VertexBuffer quads = this.quads;
        if (shader == null || quads == null) return;

        records.prepareForFrame(cameraPos, gameTime);
        setUniforms(shader, cameraPos, gameTime, partialTick);
        RenderSystem.setShaderTexture(0, type.texture());
        setupBlendState();

        // ShaderInstance only knows 2D samplers, so the record buffer is bound by hand after apply()
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, modelView, projection, mc.getWindow());
        shader.apply();
        if (recordSamplerLocation != -1) GL20.glUniform1i(recordSamplerLocation, RECORD_SAMPLER_UNIT);
        records.bind(RECORD_SAMPLER_UNIT);
        try {
            quads.bind();
            quads.draw();
        } finally {
            VertexBuffer.unbind();
            records.unbind(RECORD_SAMPLER_UNIT);
            shader.clear();
        }
    }

    private void setUniforms(ShaderInstance shader, Vec3 cameraPos, long gameTime, float partialTick) {
        Vec3 origin = records.origin();
        shader.safeGetUniform("Origin").set((float) (origin.x - cameraPos.x), (float) (origin.y - cameraPos.y), (float) (origin.z - cameraPos.z));
        shader.safeGetUniform("Time").set((gameTime - records.timeBase()) + partialTick);
        shader.safeGetUniform("Gravity").set(type.gravity());
        shader.safeGetUniform("Drag").set(dragOf(type.friction()));
        shader.safeGetUniform("SizeEnd").set(type.sizeEnd().orElse(-1f));
        shader.safeGetUniform("UseColorEnd").set(type.colorEnd().isPresent() ? 1 : 0);
        setColor(shader.safeGetUniform("ColorEnd"), type.colorEnd().orElse(-1));
        shader.safeGetUniform("Fade").set(type.fade().in(), type.fade().out());
        shader.safeGetUniform("Sway").set(type.sway());
        shader.safeGetUniform("Spin").set(type.spin());
        shader.safeGetUniform("Aspect").set(type.aspect());
        shader.safeGetUniform("Billboard").set(billboardOf(type.rotationMode()));
        shader.safeGetUniform("Frames").set(type.frames());
        shader.safeGetUniform("RandomSprite").set(type.randomSprite() ? 1 : 0);
        shader.safeGetUniform("AlphaCutoff").set(isTranslucent() ? 0.003f : 0.1f);
        for (var e : type.uniforms().entrySet()) {
            shader.safeGetUniform(e.getKey()).set((float) e.getValue().evaluate());
        }
    }

    private void setupBlendState() {
        switch (type.renderType()) {
            case TRANSLUCENT -> {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
            }
            case ADDITIVE_TRANSLUCENT -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.depthMask(false);
            }
            default -> {
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
            }
        }
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

    private static void setColor(AbstractUniform uniform, int argb) {
        uniform.set(FastColor.ARGB32.red(argb) / 255f, FastColor.ARGB32.green(argb) / 255f,
                FastColor.ARGB32.blue(argb) / 255f, FastColor.ARGB32.alpha(argb) / 255f);
    }

    @Override
    public void close() {
        if (shader != null) shader.close();
        if (quads != null) quads.close();
        records.close();
        shader = null;
        quads = null;
    }
}
