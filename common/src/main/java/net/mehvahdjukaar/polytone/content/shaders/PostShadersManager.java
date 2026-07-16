package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Manages Polytone-defined post-shader effects.
 *
 * <p>Effects are defined via {@code assets/<ns>/polytone/post_chains/*.json} (or the legacy
 * {@code post_shaders} folder) and refer to chain JSONs at {@code assets/<ns>/post_effect/<name>.json},
 * the same location 1.21.11 uses, so packs can share files.</p>
 *
 * <p>Polytone post-chains are processed as additional passes after the vanilla
 * {@code gameRenderer.postEffect}, never replacing or modifying it - so other mods
 * that toggle the vanilla post effect (e.g. spectator-mode shaders) keep working.</p>
 */
public class PostShadersManager extends JsonPartialReloader<PostChainEffect> {

    /**
     * Set on the render thread while we're constructing a {@link PostChain} for a polytone effect.
     * Loader-side mixins (e.g. fabric {@code EffectInstanceMixin}) read this and only intervene when
     * it's true, so vanilla and other mods' shader loading is left strictly untouched.
     */
    public static final ThreadLocal<Boolean> POLYTONE_LOADING = ThreadLocal.withInitial(() -> false);

    private final List<PostChainEffect> effects = new ArrayList<>();
    /** Currently loaded chains, in render order. Keyed by effect instance. */
    private final LinkedHashMap<PostChainEffect, PostChain> activeChains = new LinkedHashMap<>();
    /** Post chain IDs we already failed to load; skipped (silently) on subsequent ticks. */
    private final Set<ResourceLocation> failedChains = new HashSet<>();

    /**
     * Per-pass frame state for {@link net.mehvahdjukaar.polytone.mixins.PostPassMixin}. Set for the
     * duration of each {@code PostChain.process()} call so uniforms are applied immediately before
     * {@code EffectInstance.apply()} on every pass.
     */
    public static final ThreadLocal<ActivePostPassFrame> ACTIVE_POST_PASS = new ThreadLocal<>();

    /** Whether {@link #captureLevelDepthSnapshot()} already copied level depth this frame. */
    private boolean depthCapturedThisFrame = false;

    /**
     * Level projection / camera matrices captured during {@code GameRenderer.renderLevel}, exposed to
     * pass shaders as the {@code PolyProjMat} / {@code PolyModelViewMat} built-in uniforms.
     */
    private final Matrix4f projMat = new Matrix4f();
    private final Matrix4f modelViewMat = new Matrix4f();

    /**
     * Standalone depth target for effects that declare {@code use_depth_buffer}. We can't sample the
     * main framebuffer's own depth attachment while the post quad writes to it (read/write feedback
     * loop), so we blit the level depth into this snapshot once per frame and sample that instead.
     */
    private TextureTarget depthSnapshot = null;

    /** Fullscreen depth-only shader that folds the held-item depth into {@link #depthSnapshot}. */
    private ShaderInstance depthCombineShader = null;
    private boolean depthCombineFailed = false;

    public PostShadersManager() {
        super("Post shader", () -> SchemaCodec.wrap(PostChainEffect.CODEC), "post_chains", "post_shaders");
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        synchronized (effects) {
            closeAllChains();
            effects.clear();
            failedChains.clear();
            for (var e : jsons.entrySet()) {
                ResourceLocation id = e.getKey();
                JsonElement json = e.getValue();
                try {
                    var result = PostChainEffect.CODEC.parse(JsonOps.INSTANCE, json);
                    if (result.isError()) {
                        Polytone.LOGGER.warn("Failed to parse post shader '{}': {}", id, result.error().get().message());
                        continue;
                    }
                    effects.add(result.getOrThrow());
                } catch (Exception ex) {
                    // Belt-and-suspenders: never let one bad polytone post_shaders/*.json file
                    // abort the reload for the rest of the pack.
                    Polytone.LOGGER.warn("Failed to parse post shader '{}': {}", id, ex.getMessage());
                }
            }
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        synchronized (effects) {
            closeAllChains();
            effects.clear();
            failedChains.clear();
            Polytone.POST_TARGETS.close();
        }
    }

    /** Evaluate conditions and create/close chains accordingly. Call once per client tick. */
    public void tick() {
        synchronized (effects) {
            // Build the desired set (in render order) honouring priority
            List<PostChainEffect> desired = new ArrayList<>();
            for (PostChainEffect e : effects) {
                if (e.shouldBeOn()) desired.add(e);
            }
            desired.sort(Comparator.comparing(PostChainEffect::priority));

            // Close any chains we no longer want
            Map<PostChainEffect, PostChain> keep = new IdentityHashMap<>();
            for (var entry : activeChains.entrySet()) {
                if (desired.contains(entry.getKey())) {
                    keep.put(entry.getKey(), entry.getValue());
                } else {
                    try { entry.getValue().close(); } catch (Exception ignored) {}
                }
            }

            // Open any new chains, preserving desired order
            activeChains.clear();
            for (PostChainEffect e : desired) {
                if (failedChains.contains(e.postChain())) continue; // skip - already logged once
                PostChain chain = keep.get(e);
                if (chain == null) {
                    chain = tryLoadChain(e);
                    if (chain == null) {
                        failedChains.add(e.postChain());
                        continue;
                    }
                }
                activeChains.put(e, chain);
            }
        }
    }

    private PostChain tryLoadChain(PostChainEffect effect) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation chainRl = effect.chainResource();

        // Peek at the chain JSON first so we can reject 1.21.2+ format packs with a clear message
        // instead of letting vanilla blow up with an opaque parse error mid-load.
        Optional<Resource> res = mc.getResourceManager().getResource(chainRl);
        if (res.isEmpty()) {
            Polytone.LOGGER.error("Post shader chain '{}' not found at {}", effect.postChain(), chainRl);
            return null;
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            if (isNewFormatChain(root)) {
                Polytone.LOGGER.error(
                        "Post shader chain '{}' uses the 1.21.2+ post_effect format (fragment_shader / object-shaped targets / UBO-grouped uniforms). " +
                                "Polytone for 1.21.1 only supports the 1.21.1 post-chain format (passes with 'name', flat 'uniforms' array, 'shaders/program/...json' programs). " +
                                "Either downgrade the pack's shaders to the 1.21.1 format or use a polytone build for the matching MC version. Skipping this chain.",
                        effect.postChain());
                return null;
            }
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to read post shader chain '{}': {}", effect.postChain(), ex.getMessage());
            return null;
        }

        POLYTONE_LOADING.set(true);
        try {
            PostChain chain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    chainRl
            );
            chain.resize(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
            return chain;
        } catch (IOException ex) {
            Polytone.LOGGER.error("Failed to load post shader chain '{}': {}", effect.postChain(), ex.getMessage());
            return null;
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to parse post shader chain '{}'", effect.postChain(), ex);
            return null;
        } finally {
            POLYTONE_LOADING.set(false);
        }
    }

    /**
     * Detects the 1.21.2+ post-effect schema. Signals: {@code targets} is a JSON object (rather
     * than an array of strings/objects), or any pass declares {@code fragment_shader} (the new
     * format's replacement for the 1.21.1 {@code name} field).
     */
    private static boolean isNewFormatChain(JsonObject root) {
        JsonElement targets = root.get("targets");
        if (targets != null && targets.isJsonObject()) return true;
        JsonElement passes = root.get("passes");
        if (passes != null && passes.isJsonArray()) {
            for (JsonElement p : passes.getAsJsonArray()) {
                if (p.isJsonObject() && p.getAsJsonObject().has("fragment_shader")) return true;
            }
        }
        return false;
    }

    /** Resize all active chains to the new framebuffer dimensions. */
    public void resize(int width, int height) {
        synchronized (effects) {
            for (PostChain c : activeChains.values()) {
                c.resize(width, height);
            }
            if (depthSnapshot != null) {
                depthSnapshot.resize(width, height, Minecraft.ON_OSX);
            }
        }
    }

    /**
     * Capture the level projection and camera (model-view) matrices. Called from the
     * {@code GameRenderer.renderLevel} mixin so the {@code PolyProjMat} / {@code PolyModelViewMat}
     * built-in uniforms reflect the current frame's view.
     */
    public void captureLevelMatrices(Matrix4f projection, Matrix4f modelView) {
        this.projMat.set(projection);
        this.modelViewMat.set(modelView);
    }

    /**
     * Snapshot the main framebuffer's depth while level geometry is still intact. Called from
     * {@code LevelRenderer.renderLevel} at return - before {@code GameRenderer} clears depth for
     * first-person hand rendering.
     */
    public void captureLevelDepthSnapshot() {
        synchronized (effects) {
            if (!anyActiveEffectUsesDepth()) return;

            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.getMainRenderTarget();
            ensureDepthSnapshot(main);
            depthSnapshot.copyDepthFrom(main);
            main.bindWrite(false);
            depthCapturedThisFrame = true;
        }
    }

    /**
     * Process all active Polytone post-shader chains. Each chain reads from and writes back to the
     * main render target, so subsequent chains see the previous chain's output.
     *
     * <p>Called from the {@code GameRenderer.render} mixin after vanilla's post-effect (if any) finishes.</p>
     */
    public void renderAfterMainPostEffect(float partialTicks) {
        synchronized (effects) {
            if (activeChains.isEmpty()) return;

            Minecraft mc = Minecraft.getInstance();
            // Keep persistent post targets allocated/sized to the frame so target_samplers resolve.
            Polytone.POST_TARGETS.ensureAllocated(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
            float sunAngle = 0f;
            float dayTime = 0f;
            // frame delta time in ticks (matches 1.21.11 PolyDeltaTime = deltaTracker.getGameTimeDeltaTicks())
            float deltaTime = mc.getTimer().getGameTimeDeltaTicks();
            ClientLevel level = mc.level;
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            if (level != null) {
                // match 1.21.11: 0 = noon (sun straight up), measured from the horizon
                sunAngle = level.getSunAngle(partial) - Mth.HALF_PI;
                dayTime = (float) (level.getDayTime() % 24000L);
            }

            // lerped player (feet) position, split like vanilla's CameraBlockPos/CameraOffset so shaders
            // keep float precision at large coordinates: exact = vec3(PolyPlayerBlockPos) - PolyPlayerOffset
            Vec3 playerPos = mc.player == null ? Vec3.ZERO : mc.player.getPosition(partial);
            BlockPos playerBlockPos = BlockPos.containing(playerPos);
            Vec3 playerOffset = new Vec3(
                    playerBlockPos.getX() - playerPos.x,
                    playerBlockPos.getY() - playerPos.y,
                    playerBlockPos.getZ() - playerPos.z);

            IntSupplier depthTexture = prepareDepthSnapshot(mc);

            // The depth snapshot is taken at the end of level rendering, before GameRenderer clears the
            // depth buffer to draw the first-person hand. So held items (a raised shield) aren't in the
            // depth that effects like godrays sample, and they leak straight through. Fold the hand depth
            // back into the snapshot here (we run after the hand) so held items occlude depth effects.
            if (depthTexture != null && depthCapturedThisFrame
                    && Polytone.CONFIGS.postShadersOccludeHeldItems.get()) {
                foldHeldItemDepthIntoSnapshot(mc);
            }

            for (var entry : activeChains.entrySet()) {
                PostChainEffect effect = entry.getKey();
                PostChain chain = entry.getValue();
                ACTIVE_POST_PASS.set(new ActivePostPassFrame(
                        effect, projMat, modelViewMat, sunAngle, dayTime,
                        deltaTime, playerBlockPos, playerOffset, depthTexture));
                try {
                    chain.process(partialTicks);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Error processing polytone post chain '{}'", chain.getName(), e);
                } finally {
                    ACTIVE_POST_PASS.remove();
                }
            }

            // Every PostChain.process() ends by unbinding its final pass's output target, which leaves
            // framebuffer 0 (the default backbuffer) bound - NOT the main render target. Vanilla restores
            // the main target right after its own gameRenderer.postEffect.process() via bindWrite(true);
            // because we run our chains AFTER that restore, we must re-bind it ourselves. Otherwise the
            // entire HUD (hotbar, inventory, F3, toasts, screens) is rendered into the backbuffer and then
            // overwritten by the end-of-frame blit of the main target - i.e. the GUI vanishes.
            mc.getMainRenderTarget().bindWrite(true);

            depthCapturedThisFrame = false;
        }
    }

    /**
     * If any active effect samples the depth buffer, return a supplier of the snapshot depth texture id.
     * Prefer the copy taken at the end of {@code LevelRenderer.renderLevel}; fall back to copying now
     * when level rendering did not run this frame.
     */
    private IntSupplier prepareDepthSnapshot(Minecraft mc) {
        if (!anyActiveEffectUsesDepth()) return null;

        RenderTarget main = mc.getMainRenderTarget();
        ensureDepthSnapshot(main);
        if (!depthCapturedThisFrame) {
            depthSnapshot.copyDepthFrom(main);
            main.bindWrite(false);
        }
        return depthSnapshot::getDepthTextureId;
    }

    /**
     * Draw the (hand-only) main depth into the world-depth snapshot with a LEQUAL depth test, leaving
     * {@code min(worldDepth, handDepth)} per pixel. Runs after the first-person hand has been drawn, so
     * held items now occlude depth-driven post effects instead of leaking through them.
     */
    private void foldHeldItemDepthIntoSnapshot(Minecraft mc) {
        ShaderInstance shader = getDepthCombineShader(mc);
        if (shader == null) return;

        RenderTarget main = mc.getMainRenderTarget();

        depthSnapshot.bindWrite(true);

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

        // Restore neutral state; the chain loop and vanilla's later HUD pass set up their own.
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        main.bindWrite(false);
    }

    private ShaderInstance getDepthCombineShader(Minecraft mc) {
        if (depthCombineShader == null && !depthCombineFailed) {
            try {
                depthCombineShader = new ShaderInstance(mc.getResourceManager(),
                        "polytone_depth_combine", DefaultVertexFormat.POSITION_TEX);
            } catch (Exception e) {
                depthCombineFailed = true;
                Polytone.LOGGER.error("Failed to load polytone_depth_combine shader; " +
                        "held items will not occlude depth-driven post shaders", e);
            }
        }
        return depthCombineShader;
    }

    private boolean anyActiveEffectUsesDepth() {
        for (PostChainEffect e : activeChains.keySet()) {
            if (e.useDepthBuffer()) return true;
        }
        return false;
    }

    /**
     * Whether any active chain declares {@code use_shadow_map}. Read from the render thread
     * ({@code LevelRenderer.renderLevel} TAIL) to decide whether to render the shadow depth map.
     */
    public boolean anyActiveEffectUsesShadowMap() {
        synchronized (effects) {
            for (PostChainEffect e : activeChains.keySet()) {
                if (e.useShadowMap()) return true;
            }
            return false;
        }
    }

    private void ensureDepthSnapshot(RenderTarget main) {
        if (depthSnapshot == null) {
            depthSnapshot = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            depthSnapshot.setClearColor(0f, 0f, 0f, 0f);
        } else if (depthSnapshot.width != main.width || depthSnapshot.height != main.height) {
            depthSnapshot.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        // On (Neo)Forge a mod can call RenderTarget.enableStencil() on the main framebuffer (several of
        // MehVahd's own mods do), which flips its depth attachment from GL_DEPTH_COMPONENT to a combined
        // GL_DEPTH32F_STENCIL8. copyDepthFrom() blits GL_DEPTH_BUFFER_BIT, and that blit requires source and
        // destination depth formats to match - otherwise GL raises INVALID_OPERATION and copies nothing, so
        // the snapshot stays cleared and depth-driven effects (godrays, etc.) silently do nothing. Vanilla's
        // own PostChain.addTempTarget propagates stencil to its temp targets; mirror the same onto our
        // snapshot. The stencil API is Forge-only, hence the platform hop (no-op on Fabric).
        PlatStuff.matchStencil(main, depthSnapshot);
    }

    public record ActivePostPassFrame(
            PostChainEffect effect,
            Matrix4f projMat,
            Matrix4f modelViewMat,
            float sunAngle,
            float dayTime,
            float deltaTime,
            BlockPos playerBlockPos,
            Vec3 playerOffset,
            IntSupplier depthTexture
    ) {}

    private void closeAllChains() {
        for (PostChain c : activeChains.values()) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        activeChains.clear();
        if (depthSnapshot != null) {
            depthSnapshot.destroyBuffers();
            depthSnapshot = null;
        }
        if (depthCombineShader != null) {
            depthCombineShader.close();
            depthCombineShader = null;
        }
        depthCombineFailed = false;
        Polytone.SHADOWS.close();
    }
}
