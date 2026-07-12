package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomParticleInstance extends TextureSheetParticle {

    public final CustomParticleType type;
    protected final @Nullable BakedModel model;
    protected final List<IParticleTickable> tickables;
    protected float oQuadSize;
    protected double custom;

    private boolean inFrustumLastTick = true;

    private Quaternionf customRotation = null;
    private Quaternionf customRotationO = null;

    // newborn awaiting its spawn-time ticker pass in the parallel batch (see PolytoneAsyncParticles)
    boolean pendingInitTick = false;

    // light cache: re-samples only when the particle crosses a block, or its section changed
    private final ParticleLightCache.Entry lightCache;
    private final @Nullable ParticleColor.Cache colormapCache;

    protected CustomParticleInstance(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                     @Nullable BlockState state, CustomParticleType customType) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.type = customType;
        this.hasPhysics = customType.hasPhysics;

        this.setSprite(customType.spritePicker.getAny());
        this.type.spritePicker.pickSprite(this, true);

        this.setSize(0.1f, 0.1f);

        this.tickables = new ArrayList<>();
        this.tickables.addAll(customType.sounds);
        this.tickables.addAll(customType.particleEmitters);

        //for normal particles since its simple particle types (so that they can be ued in biomes) we can pass extra params
        if (state == null) state = STATE_HACK;

        // remove randomness
        this.x = x;
        this.y = y;
        this.z = z;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        BakedModel bm = null;
        if (customType.model != null) {
            bm = PlatStuff.getBakedModel(new ModelResourceLocation(customType.model, "standalone"));
            if (bm == null) {
                Polytone.LOGGER.error("Failed to load custom particle model: {}. Maybe model was missing", customType.model);
            }
        }
        this.model = bm;
        CustomParticleInitializer initializer = customType.initializer;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (initializer != null) {
            initializer.initialize(this, level, state, pos);
        }

        this.oQuadSize = quadSize;

        // seed the spawn color; the cache also records the block so PER_POSITION only re-samples
        // once the particle moves off it. null when this type has no colormap (the common case)
        this.colormapCache = this.type.colormap == null ? null
                : new ParticleColor.Cache(this, this.type.colormap, state, pos);
        this.lightCache = new ParticleLightCache.Entry(super::getLightColor);

    }

    public Level getLevel() {
        return level;
    }


    public void notifyInFrustum(boolean wasInFrustum) {
        this.inFrustumLastTick = wasInFrustum;
    }

    public boolean hasAgeLeft() {
        return this.age < this.lifetime;
    }

    public double getCustom() {
        return custom;
    }

    public void setCustom(double custom) {
        this.custom = custom;
    }

    @Override
    public @NotNull Optional<ParticleGroup> getParticleGroup() {
        return this.type.particleGroupLimit;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Fast path: when this particle is just a plain camera-facing textured quad we defer to the
        // vanilla SingleQuadParticle.render, which Sodium hijacks at HEAD to write straight into its
        // packed particle buffer. getLightColor/getU0..V1 stay virtual there, so our sprite and
        // light-level overrides still apply. When Sodium is absent this is simply the vanilla path.
        if (canUseSodiumFastPath()) {
            super.render(buffer, camera, partialTicks);
            return;
        }

        Quaternionf quaternionf = new Quaternionf();
        IRotationProvider rotProv = this.type.rotationProvider;
        if (rotProv.updatesEveryRenderTick() || this.customRotationO == null) {
            // no cached rotation yet, eval directly
            rotProv.setRotation(this, quaternionf, camera, partialTicks);
        } else {
            // 3-arg slerp writes to dest, keeps customRotationO intact for next frame
            this.customRotationO.slerp(this.customRotation, partialTicks, quaternionf);
        }

        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }

        this.renderRotatedQuad(buffer, camera, quaternionf, partialTicks);
        if (!rotProv.alwaysFacesCamera() && model == null) {
            quaternionf.rotateX(Mth.PI);
            //render back face
            this.renderRotatedQuad(buffer, camera, quaternionf, partialTicks);
        }
    }

    // True only when our particle is exactly the quad Sodium's fast path builds, so we can safely let
    // super.render take over. Requirements: LOOK_AT_XYZ specifically (Sodium billboards with camera
    // left/up), no model, zero offset, and a render mode that neither redirects the consumer nor is
    // one Sodium mis-renders (TRANSLUCENT / ADDITIVE_TRANSLUCENT).
    private boolean canUseSodiumFastPath() {
        return this.model == null
                && this.type.rotationProvider == RotationMode.LOOK_AT_XYZ
                && this.type.offset.lengthSqr() == 0
                && this.type.renderType != ParticleRenderMode.TRANSLUCENT
                && this.type.renderType != ParticleRenderMode.ADDITIVE_TRANSLUCENT;
    }

    // Sodium 0.8.x injects into SingleQuadParticle.renderRotatedQuad(VertexConsumer, Camera, Quaternionf, float)
    // and cancels it at HEAD. Overriding this overload keeps dispatch out of the mixed-in superclass method
    // whenever we take the slow path (buffer redirection, offset, model rendering).
    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Camera camera, Quaternionf quaternion, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        this.renderRotatedQuad(buffer, quaternion, x, y, z, partialTicks);
    }

    @Override
    protected void renderRotatedQuad(VertexConsumer consumer, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
        Vec3 offset = this.type.offset;
        if (model == null) {
            consumer = this.type.renderType.modifyParticleConsumer(consumer);
            super.renderRotatedQuad(consumer, quaternion, (float) (x + offset.x),
                    (float) (y + offset.y), (float) (z + offset.z), partialTicks);
        } else {
            consumer = this.type.renderType.modifyBlockConsumer(consumer);

            float size = this.getQuadSize(partialTicks);

            PoseStack poseStack = new PoseStack();
            poseStack.translate(x + offset.x, y + offset.y, z + offset.z);

            poseStack.scale(size, size, size);
            poseStack.mulPose(quaternion);
            poseStack.translate(-0.5, -0.5, -0.5);

            putModelBulkData(this.model, this.getLightColor(partialTicks),
                    OverlayTexture.NO_OVERLAY, poseStack, consumer, this.rCol, this.gCol, this.bCol, this.alpha);
        }
    }

    public static void putModelBulkData(BakedModel model, int combinedLight, int combinedOverlay,
                                        PoseStack poseStack, VertexConsumer buffer, float r, float g, float b, float a) {
        RandomSource randomSource = RandomSource.create();
        for (Direction direction : Direction.values()) {
            randomSource.setSeed(42L);
            for (BakedQuad bakedQuad : model.getQuads(null, direction, randomSource)) {
                buffer.putBulkData(poseStack.last(), bakedQuad, r, g, b, a, combinedLight, combinedOverlay);
            }
        }
        randomSource.setSeed(42L);
        for (BakedQuad bakedQuad : model.getQuads(null, null, randomSource)) {
            buffer.putBulkData(poseStack.last(), bakedQuad, r, g, b, a, combinedLight, combinedOverlay);
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        int total = lightCache.get(this.x, this.y, this.z, partialTick);
        if (this.type.lightLevel > 0) {
            int sky = LightTexture.sky(total);
            int block = LightTexture.block(total);
            block = Math.max(block, this.type.lightLevel);
            return LightTexture.pack(block, sky);
        }
        return total;
    }

    @Override
    public void remove() {
        super.remove();
        this.age = this.lifetime;
    }

    @Override
    public void setSprite(TextureAtlasSprite textureAtlasSprite) {
        super.setSprite(textureAtlasSprite);
    }

    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public void tick() {
        // Lifetime expires synchronously: the engine culls right after tick(), a deferred check
        // makes every particle live one tick longer. super.tick()'s own check then never fires.
        if (this.removed) return; // removed off-thread last tick, culled right after this call
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        if (Polytone.CONFIGS.particlesOffThread.get()) {
            PolytoneAsyncParticles.enqueue(this); // physics + expression work runs in the parallel batch
        } else tickInternal();
    }

    /**
     * Spawn-time ticker pass for a newborn; deferred to the parallel batch when async is on.
     */
    void initTick() {
        this.type.ticker.tick(this, level);
        this.setAge(0); // reset so the spawn-time pass doesn't age the particle
    }

    void tickInternal() {
        if (!this.type.isValid()) {
            this.remove();
            return;
        }
        if (type.killWhenNotInView && !this.inFrustumLastTick) {
            this.remove();
            return;
        }
        if (type.killWhenNotInView && isBehindCamera()) {
            this.remove();
            return;
        }

        this.type.spritePicker.pickSprite(this, false);
        super.tick();
        //interpolate our states
        this.oRoll = this.roll;
        this.oQuadSize = this.quadSize;

        if (!this.type.rotationProvider.updatesEveryRenderTick()) {
            //handle initialized state where both are null
            Quaternionf instantRot = new Quaternionf();
            this.type.rotationProvider.setRotation(this, instantRot,
                    PolytoneAsyncParticles.camera(), 0);
            if (this.customRotation == null || this.customRotationO == null) {
                this.customRotation = new Quaternionf(instantRot);
                this.customRotationO = new Quaternionf(instantRot);
            } else {
                this.customRotationO.set(this.customRotation);
                this.customRotation.set(instantRot);
            }
        }

        boolean isTickTime = this.age % this.type.tickRate == 0;

        if (type.ticker != null && isTickTime) {
            type.ticker.tick(this, level);
        }

        // colormap: sampled once at spawn (see constructor); the cache policy decides whether to
        // re-evaluate here (ON_SPAWN freezes, PER_POSITION on block change, NONE every tick)
        if (colormapCache != null) colormapCache.tick();

        if (this.age > 1 && type.killWhenStill && this.x == this.xo && this.y == this.yo && this.z == this.zo) {
            this.remove();
        }

        if (type.liquidAffinity != LiquidAffinity.ANY) {
            BlockPos pos = BlockPos.containing(x, y, z);
            // off-thread: skip unloaded chunks, otherwise the air fallback would wrongly kill the particle
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                if (type.liquidAffinity == LiquidAffinity.LIQUIDS ^ !state.getFluidState().isEmpty()) {
                    this.remove();
                }
            }
        }
        if (!this.removed && isTickTime) {
            for (IParticleTickable tickable : this.tickables) {
                tickable.tick(this, level);
            }
        }
    }

    @Override
    public void move(double x, double y, double z) {
        super.move(x, y, z);
        if (!type.sticky) {
            //TODO: do it properly here. when collision stops this particle we dont move at all until the obstacle is removed, unless we would slide
            stoppedByCollision = false;
        }
        if (type.killOnContact && this.age > 1) {
            Vec3 myPos = new Vec3(this.x, this.y, this.z);
            Vec3 wantedPos = new Vec3(this.xo + x, this.yo + y, this.zo + z);
            if (myPos.distanceToSqr(wantedPos) > 0.000001) {
                // collided with any block. pop. It fragile
                this.remove();
                this.xd = 0;
                this.yd = 0;
                this.zd = 0;
            }
        }
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return Mth.lerp(scaleFactor, this.oQuadSize, this.quadSize);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.model == null ? type.renderType.getParticle() : ParticleRenderType.CUSTOM;
    }


    private static BlockState STATE_HACK = Blocks.AIR.defaultBlockState();

    public static void setStateHack(BlockState state) {
        STATE_HACK = state;
    }

    public void setAge(int i) {
        this.age = i;
    }


    private boolean isBehindCamera() {
        Camera camera = PolytoneAsyncParticles.camera();
        if (camera.getEntity() == Minecraft.getInstance().player) {
            //check distance
            Vector3f cameraPos = camera.getPosition().toVector3f();
            Vector3f thisPos = new Vector3f((float) this.x, (float) this.y, (float) this.z);
            double distance = cameraPos.distanceSquared(thisPos);

            if (distance > (4 * 4)) {
                Vector3f toObject = thisPos.sub(cameraPos);
                var lookVector = camera.getLookVector();
                double dotProduct = toObject.dot(lookVector);
                return dotProduct < 0;
            }
        }
        return false;
    }
}
