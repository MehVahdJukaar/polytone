package net.mehvahdjukaar.polytone.content.particle.custom;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.SpecialModelsHandler;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

public class CustomParticleInstance extends SingleQuadParticle {

    public final CustomParticleType type;
    protected final @Nullable QuadCollection model;
    protected final List<IParticleTickable> tickables;
    protected float oQuadSize;
    protected double custom;

    private boolean inFrustumLastTick = true;

    private Quaternionf customRotation = null;
    private Quaternionf customRotationO = null;

    // newborn awaiting its spawn-time ticker pass in the parallel batch (see PolytoneAsyncParticles)
    boolean pendingInitTick = false;

    // light cache: re-sample only when the particle crosses a block, or its section's light changed
    private long cachedLightKey = Long.MIN_VALUE;
    private int cachedLightVersion;
    private int cachedRawLight;

    protected CustomParticleInstance(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                     @Nullable BlockState state, CustomParticleType customType) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, customType.spritePicker.getAny());
        this.type = customType;
        this.hasPhysics = customType.hasPhysics;

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
        QuadCollection qc = null;
        if (customType.model != null) {
            qc = SpecialModelsHandler.getSpecialModel(customType.model);
            if (qc == null) {
                Polytone.LOGGER.error("Failed to load custom particle model: {}. Maybe model was missing", customType.model);
            }
        }
        this.model = qc;
        CustomParticleInitializer initializer = customType.initializer;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (initializer != null) {
            initializer.initialize(this, level, state, pos);
        }

        this.oQuadSize = quadSize;

        if (this.type.colormap != null) {
            float[] unpack = ColorUtils.unpack(this.type.colormap.getColor(state, level, pos, 0));
            this.setColor(unpack[0], unpack[1], unpack[2]);
        }

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
    public @NotNull Optional<ParticleLimit> getParticleLimit() {
        return this.type.particleGroupLimit;
    }

    //Unused. Why? because we need extra context of the particle iteself to be passed in apply rotation
    @Override
    public FacingCameraMode getFacingCameraMode() {
        return this.type.rotationProvider;
    }

    @Override
    public void extract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f) {
        Quaternionf quaternionf = new Quaternionf();
        IRotationProvider rotProv = this.type.rotationProvider;
        if (rotProv.updatesEveryRenderTick() || this.customRotationO == null) {
            // no cached rotation yet, eval directly
            rotProv.setRotation(this, quaternionf, camera, f);
        } else {
            // 3-arg slerp writes to dest, keeps customRotationO intact for next frame
            this.customRotationO.slerp(this.customRotation, f, quaternionf);
        }

        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(f, this.oRoll, this.roll));
        }
        this.extractRotatedQuad(quadParticleRenderState, camera, quaternionf, f);
        if (!rotProv.alwaysFacesCamera() && model == null) {
            quaternionf.rotateX(Mth.PI);
            //render back face
            this.extractRotatedQuad(quadParticleRenderState, camera, quaternionf, f);
        }
    }

    public void extractModel(ModelParticleRenderState modelParticleRenderState, Camera camera, float f) {
        if (this.model == null) {
            //failsafe
            return;
        }
        Quaternionf quaternionf = new Quaternionf();
        this.type.rotationProvider.setRotation(this, quaternionf, camera, f);
        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(f, this.oRoll, this.roll));
        }
        var offset = this.type.offset;
        modelParticleRenderState.add(
                this.type.renderType,
                (float) (x + offset.x), (float) (y + offset.y), (float) (z + offset.z),
                quaternionf.x,
                quaternionf.y,
                quaternionf.z,
                quaternionf.w,
                this.getQuadSize(f),
                this.rCol, this.gCol, this.bCol, this.alpha,
                this.getLightColor(f),
                this.model
        );
    }

    @Override
    protected void extractRotatedQuad(QuadParticleRenderState quadParticleRenderState,
                                      Quaternionf rot, float x, float y, float z, float f) {
        var offset = this.type.offset;
        super.extractRotatedQuad(quadParticleRenderState, rot,
                (float) (x + offset.x), (float) (y + offset.y), (float) (z + offset.z), f);
    }

    @Override
    protected int getLightColor(float partialTick) {
        int bx = Mth.floor(this.x), by = Mth.floor(this.y), bz = Mth.floor(this.z);
        long blockKey = BlockPos.asLong(bx, by, bz);
        int version = ParticleLightCache.sectionVersion(bx >> 4, by >> 4, bz >> 4);
        if (blockKey != this.cachedLightKey || version != this.cachedLightVersion) {
            this.cachedLightKey = blockKey;
            this.cachedLightVersion = version;
            this.cachedRawLight = super.getLightColor(partialTick); // the expensive world/light lookup
        }
        int total = this.cachedRawLight;
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

    /** Spawn-time ticker pass for a newborn; deferred to the parallel batch when async is on. */
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

        // colormap color is sampled once at spawn (see constructor) and never re-evaluated

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
    protected Layer getLayer() {
        return type.renderType.getLayer(model != null);
    }


    @Override
    public ParticleRenderType getGroup() {
        return this.type.getParticleGroup();
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
        if (camera.entity() == Minecraft.getInstance().player) {
            //check distance
            Vector3f cameraPos = camera.position().toVector3f();
            Vector3f thisPos = new Vector3f((float) this.x, (float) this.y, (float) this.z);
            double distance = cameraPos.distanceSquared(thisPos);

            if (distance > (4 * 4)) {
                camera.forwardVector();
                Vector3f toObject = thisPos.sub(cameraPos);
                var lookVector = camera.forwardVector();
                double dotProduct = toObject.dot(lookVector);
                return dotProduct < 0;
            }
        }
        return false;
    }
}
