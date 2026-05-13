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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomParticleInstance extends SingleQuadParticle {

    private static final ExecutorService PARTICLE_THREAD = Executors.newWorkStealingPool();

    public final CustomParticleType type;
    protected final @Nullable QuadCollection model;
    protected final List<IParticleTickable> tickables;
    protected float oQuadSize;
    protected double custom;

    private boolean inFrustumLastTick = true;

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
        this.type.rotationProvider.setRotation(this, quaternionf, camera, f);
        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(f, this.oRoll, this.roll));
        }
        this.extractRotatedQuad(quadParticleRenderState, camera, quaternionf, f);
        if (!this.type.rotationProvider.alwaysFacesCamera() && model == null) {
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
        int total = super.getLightColor(partialTick);
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
        if (Polytone.CONFIGS.particlesOffThread.get()) {
            PARTICLE_THREAD.submit(this::tickInternal);
        } else tickInternal();
    }

    private void tickInternal() {
        if(this.type.debugId.toString().contains("lightray") && !this.type.debugId.toString().contains("emitter")){
            int aa = 1;
        }
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

        boolean isTickTime = this.age % this.type.tickRate == 0;

        if (type.ticker != null && isTickTime) {
            type.ticker.tick(this, level);
        }

        if (this.type.colormap != null) {
            BlockPos pos = BlockPos.containing(x, y, z);
            float[] unpack = ColorUtils.unpack(this.type.colormap.getColor(null, level, pos, 0));
            this.setColor(unpack[0], unpack[1], unpack[2]);
        }

        if (this.age > 1 && type.killWhenStill && this.x == this.xo && this.y == this.yo && this.z == this.zo) {
            this.remove();
        }

        if (type.liquidAffinity != LiquidAffinity.ANY) {
            BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
            if (type.liquidAffinity == LiquidAffinity.LIQUIDS ^ !state.getFluidState().isEmpty()) {
                this.remove();
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
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
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
