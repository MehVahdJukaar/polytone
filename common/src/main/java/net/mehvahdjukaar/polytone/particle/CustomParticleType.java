package net.mehvahdjukaar.polytone.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.sound.ParticleSoundEmitter;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ModelResHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomParticleType implements CustomParticleFactory {

    private static BlockState STATE_HACK = Blocks.AIR.defaultBlockState();

    private final RenderType renderType;
    private final @Nullable ModelResourceLocation model;
    private final @Nullable ParticleInitializer initializer;
    private final @Nullable Ticker ticker;
    private final List<ParticleSoundEmitter> sounds;
    protected final List<ParticleParticleEmitter> particles = new ArrayList<>();
    @Nullable
    protected List<Dynamic<?>> lazyParticles;

    private final int lightLevel;
    private final LiquidAffinity liquidAffinity;
    private final boolean hasPhysics;
    private final boolean killOnContact;
    private final @Nullable IColorGetter colormap;
    private final RotationMode rotationMode;
    private final Vec3 offset;

    private transient SpriteSet spriteSet;

    private CustomParticleType(RenderType renderType, RotationMode rotationMode,
                               @Nullable ResourceLocation model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact,
                               LiquidAffinity liquidAffinity, @Nullable IColorGetter colormap,
                               @Nullable ParticleInitializer initializer, @Nullable Ticker ticker,
                               @Nullable List<ParticleSoundEmitter> sounds, @Nullable List<Dynamic<?>> particles) {
        this.renderType = renderType;
        this.model = model == null ? null : new ModelResourceLocation(model, "standalone");
        this.initializer = initializer;
        this.ticker = ticker;
        this.sounds = sounds;
        this.lazyParticles = particles;
        this.lightLevel = light;
        this.hasPhysics = hasPhysics;
        this.killOnContact = killOnContact;
        this.liquidAffinity = liquidAffinity;
        this.colormap = colormap;
        this.offset = offset;
        this.rotationMode = rotationMode;
    }

    public static final Codec<CustomParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
            RenderType.CODEC.optionalFieldOf("render_type", RenderType.OPAQUE)
                    .forGetter(CustomParticleType::getRenderType),
            RotationMode.CODEC.optionalFieldOf("rotation_mode", RotationMode.LOOK_AT_XYZ).forGetter(c -> c.rotationMode),
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(c -> Optional.ofNullable(c.model.id())),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(c -> c.offset),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(c -> c.lightLevel),
            Codec.BOOL.optionalFieldOf("has_physics", true).forGetter(c -> c.hasPhysics),
            Codec.BOOL.optionalFieldOf("kill_on_contact", false).forGetter(c -> c.killOnContact),
            LiquidAffinity.CODEC.optionalFieldOf("liquid_affinity", LiquidAffinity.ANY).forGetter(c -> c.liquidAffinity),
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(c -> Optional.ofNullable(c.colormap)),
            ParticleInitializer.CODEC.optionalFieldOf("initializer").forGetter(c -> Optional.ofNullable(c.initializer)),
            Ticker.CODEC.optionalFieldOf("ticker").forGetter(c -> Optional.ofNullable(c.ticker)),
            ParticleSoundEmitter.CODEC.listOf().optionalFieldOf("sound_emitters", List.of()).forGetter(c -> c.sounds),
            Codec.PASSTHROUGH.listOf().optionalFieldOf("particle_emitters", List.of()).forGetter(c -> c.lazyParticles)
    ).apply(i, CustomParticleType::new));

    private CustomParticleType(RenderType renderType, RotationMode rotationMode,
                               Optional<ResourceLocation> model, Vec3 offset,
                               int light, boolean hasPhysics, boolean killOnContact,
                               LiquidAffinity liquidAffinity, Optional<IColorGetter> colormap,
                               Optional<ParticleInitializer> initializer,
                               Optional<Ticker> ticker, List<ParticleSoundEmitter> sounds, List<Dynamic<?>> particles) {
        this(renderType, rotationMode, model.orElse(null), offset,
                light, hasPhysics, killOnContact, liquidAffinity, colormap.orElse(null), initializer.orElse(null), ticker.orElse(null), sounds, particles);
    }

    @Override
    public @Nullable ModelResourceLocation getCustomModel() {
        return this.model;
    }

    public static void setStateHack(BlockState state) {
        STATE_HACK = state;
    }

    private RenderType getRenderType() {
        return renderType;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                   @Nullable BlockState state) {
        if (spriteSet != null) {
            // some people might want this

            Instance newParticle = new Instance(world, x, y, z, xSpeed, ySpeed, zSpeed, state, this,
                    BuiltInRegistries.PARTICLE_TYPE.getKey(type));

            if (this.hasPhysics) {
                for (VoxelShape voxelShape : world.getBlockCollisions(null, newParticle.getBoundingBox())) {
                    if (!voxelShape.isEmpty()) {
                        return null;
                    }
                }
            }

            if (this.ticker != null && this.ticker.removeIf != null) {
                if (this.ticker.removeIf.getValue(newParticle, world) > 0) {
                    return null;
                }
            }
            return newParticle;
        } else {
            throw new IllegalStateException("Sprite set not set for custom particle type");
        }
    }

    @Override
    public void setSpriteSet(ParticleEngine.MutableSpriteSet mutableSpriteSet) {
        this.spriteSet = mutableSpriteSet;
    }

    public static class Instance extends TextureSheetParticle {

        protected final RenderType renderType;
        protected final @Nullable BakedModel model;
        protected final RotationMode rotationMode;
        protected final @Nullable Ticker ticker;
        protected final SpriteSet spriteSet;
        protected final LiquidAffinity liquidAffinity;
        protected final @Nullable IColorGetter colormap;
        protected final List<ParticleTickable> tickables;
        protected final int light;
        protected float oQuadSize;
        protected final Vec3 offset;
        protected double custom;
        protected boolean killOnContact;

        private ResourceLocation name;

        protected Instance(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                           @Nullable BlockState state, CustomParticleType customType, ResourceLocation typeId) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.setSize(0.1f, 0.1f);
            this.name = typeId;
            this.light = customType.lightLevel;
            this.rotationMode = customType.rotationMode;
            this.killOnContact = customType.killOnContact;
            this.colormap = customType.colormap;
            this.tickables = new ArrayList<>();
            this.tickables.addAll(customType.sounds);
            this.tickables.addAll(customType.particles);
            this.offset = customType.offset;
            //for normal particles since its simple particle types (so that they can be ued in biomes) we can pass extra params
            if (state == null) state = STATE_HACK;

            // remove randomness
            this.x = x;
            this.y = y;
            this.z = z;
            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;
            this.model = customType.model == null ? null : PlatStuff.getBakedModel(customType.model);
            this.renderType = customType.renderType;
            this.ticker = customType.ticker;
            this.spriteSet = customType.spriteSet;
            ParticleInitializer initializer = customType.initializer;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (initializer != null) {
                initializer.initialize(this, level, state, pos);
            }

            this.oQuadSize = quadSize;

            this.liquidAffinity = customType.liquidAffinity;
            this.hasPhysics = customType.hasPhysics;

            if (this.colormap != null) {
                float[] unpack = ColorUtils.unpack(this.colormap.getColor(state, level, pos, 0));
                this.setColor(unpack[0], unpack[1], unpack[2]);
            }

            this.setSpriteFromAge(spriteSet);
        }


        public double getCustom() {
            return custom;
        }

        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            Quaternionf quaternionf = new Quaternionf();
            this.rotationMode.setRotation(this, quaternionf, camera, partialTicks);
            if (this.roll != 0.0F) {
                quaternionf.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
            }

            this.renderRotatedQuad(buffer, camera, quaternionf, partialTicks);
            if (this.rotationMode.hasBackFace()) {
                quaternionf.rotateX(Mth.PI);
                //render back face
                this.renderRotatedQuad(buffer, camera, quaternionf, partialTicks);
            }
        }

        @Override
        protected void renderRotatedQuad(VertexConsumer consumer, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
            if (model == null) {
                super.renderRotatedQuad(consumer, quaternion, (float) (x + offset.x),
                        (float) (y + offset.y), (float) (z + offset.z), partialTicks);
            } else {
                float size = this.getQuadSize(partialTicks);

                PoseStack poseStack = new PoseStack();
                poseStack.translate(x + offset.x, y + offset.y, z + offset.z);

                poseStack.scale(size, size, size);
                poseStack.mulPose(quaternion);
                poseStack.translate(-0.5, -0.5, -0.5);

                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                consumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.cutout());

                putModelBulkData(this.model, this.getLightColor(partialTicks),
                        OverlayTexture.NO_OVERLAY, poseStack, consumer, this.rCol, this.gCol, this.bCol, this.alpha);

                bufferSource.endBatch();
            }
        }

        @Override
        protected int getLightColor(float partialTick) {
            int total = super.getLightColor(partialTick);
            if (this.light > 0) {
                int sky = LightTexture.sky(total);
                int block = LightTexture.block(total);
                block = Math.max(block, light);
                return LightTexture.pack(block, sky);
            }
            return total;
        }

        @Override
        public void tick() {
            this.setSpriteFromAge(spriteSet);
            super.tick();

            if (this.ticker != null) {
                this.ticker.tick(this, level);
            }

            if (this.colormap != null) {
                BlockPos pos = BlockPos.containing(x, y, z);
                float[] unpack = ColorUtils.unpack(this.colormap.getColor(null, level, pos, 0));
                this.setColor(unpack[0], unpack[1], unpack[2]);
            }

            if (this.age > 1 && this.x == this.xo && this.y == this.yo && this.z == this.zo && hasPhysics) {
                this.remove();
            }

            //TODO: check for any block collision. also check this on my mods
            if (this.hasPhysics && this.stoppedByCollision) {
                this.remove();
            }

            if (liquidAffinity != LiquidAffinity.ANY) {
                BlockState state = level.getBlockState(BlockPos.containing(x, y, z));
                if (liquidAffinity == LiquidAffinity.LIQUIDS ^ !state.getFluidState().isEmpty()) {
                    this.remove();
                }
            }
            if (!this.removed) {
                for (ParticleTickable tickable : this.tickables) {
                    tickable.tick(this, level);
                }
            }
        }

        @Override
        public void move(double x, double y, double z) {
            super.move(x, y, z);
            if (this.killOnContact && this.age > 1) {
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
        public float getQuadSize(float scaleFactor) {
            return Mth.lerp(scaleFactor, this.oQuadSize, this.quadSize);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return this.model == null ? this.renderType.getParticle() : ParticleRenderType.CUSTOM;
        }

    }

    public enum RenderType implements StringRepresentable {
        TERRAIN,
        OPAQUE,
        TRANSLUCENT,
        LIT,
        INVISIBLE;

        public static final Codec<RenderType> CODEC = StringRepresentable.fromEnum(RenderType::values);

        public net.minecraft.client.renderer.RenderType getBlock() {
            return switch (this) {
                case TERRAIN -> net.minecraft.client.renderer.RenderType.solid();
                case TRANSLUCENT -> net.minecraft.client.renderer.RenderType.translucent();
                case LIT -> net.minecraft.client.renderer.RenderType.cutout();
                case INVISIBLE -> net.minecraft.client.renderer.RenderType.cutout();
                default -> net.minecraft.client.renderer.RenderType.cutoutMipped();
            };
        }

        public ParticleRenderType getParticle() {
            return switch (this) {
                case TERRAIN -> ParticleRenderType.TERRAIN_SHEET;
                case TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
                case LIT -> ParticleRenderType.PARTICLE_SHEET_LIT;
                case INVISIBLE -> ParticleRenderType.NO_RENDER;
                default -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            };
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    //TODO: merge this and particle modifier
    protected record Ticker(@Nullable ParticleContextExpression x, @Nullable ParticleContextExpression y,
                            @Nullable ParticleContextExpression z,
                            @Nullable ParticleContextExpression dx, @Nullable ParticleContextExpression dy,
                            @Nullable ParticleContextExpression dz,
                            @Nullable ParticleContextExpression size,
                            @Nullable ParticleContextExpression red, @Nullable ParticleContextExpression green,
                            @Nullable ParticleContextExpression blue, @Nullable ParticleContextExpression alpha,
                            @Nullable ParticleContextExpression roll,
                            @Nullable ParticleContextExpression custom,
                            @Nullable ParticleContextExpression removeIf) {

        private static final Codec<Ticker> CODEC = RecordCodecBuilder.create(i -> i.group(
                ParticleContextExpression.CODEC.optionalFieldOf("x").forGetter(p -> Optional.ofNullable(p.x)),
                ParticleContextExpression.CODEC.optionalFieldOf("y").forGetter(p -> Optional.ofNullable(p.y)),
                ParticleContextExpression.CODEC.optionalFieldOf("z").forGetter(p -> Optional.ofNullable(p.z)),
                ParticleContextExpression.CODEC.optionalFieldOf("dx").forGetter(p -> Optional.ofNullable(p.dx)),
                ParticleContextExpression.CODEC.optionalFieldOf("dy").forGetter(p -> Optional.ofNullable(p.dy)),
                ParticleContextExpression.CODEC.optionalFieldOf("dz").forGetter(p -> Optional.ofNullable(p.dz)),
                ParticleContextExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.size)),
                ParticleContextExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.red)),
                ParticleContextExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.green)),
                ParticleContextExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blue)),
                ParticleContextExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alpha)),
                ParticleContextExpression.CODEC.optionalFieldOf("roll").forGetter(p -> Optional.ofNullable(p.roll)),
                ParticleContextExpression.CODEC.optionalFieldOf("custom").forGetter(p -> Optional.ofNullable(p.custom)),
                ParticleContextExpression.CODEC.optionalFieldOf("remove_condition").forGetter(p -> Optional.ofNullable(p.removeIf))
        ).apply(i, Ticker::new));

        private Ticker(Optional<ParticleContextExpression> x, Optional<ParticleContextExpression> y,
                       Optional<ParticleContextExpression> z, Optional<ParticleContextExpression> dx,
                       Optional<ParticleContextExpression> dy, Optional<ParticleContextExpression> dz,
                       Optional<ParticleContextExpression> size, Optional<ParticleContextExpression> red,
                       Optional<ParticleContextExpression> green, Optional<ParticleContextExpression> blue,
                       Optional<ParticleContextExpression> alpha, Optional<ParticleContextExpression> roll,
                       Optional<ParticleContextExpression> custom,
                       Optional<ParticleContextExpression> removeIf) {
            this(x.orElse(null), y.orElse(null),
                    z.orElse(null), dx.orElse(null),
                    dy.orElse(null), dz.orElse(null),
                    size.orElse(null), red.orElse(null),
                    green.orElse(null), blue.orElse(null),
                    alpha.orElse(null), roll.orElse(null),
                    custom.orElse(null), removeIf.orElse(null)
            );
        }

        private void tick(CustomParticleType.Instance particle, ClientLevel level) {
            if (this.roll != null) {
                particle.oRoll = particle.roll;
                particle.roll = (float) particle.ticker.roll.getValue(particle, level);
            }
            if (this.size != null) {
                particle.oQuadSize = particle.quadSize;
                particle.quadSize = (float) this.size.getValue(particle, level);
            }
            if (this.red != null) {
                particle.rCol = (float) this.red.getValue(particle, level);
            }
            if (this.green != null) {
                particle.gCol = (float) this.green.getValue(particle, level);
            }
            if (this.blue != null) {
                particle.bCol = (float) this.blue.getValue(particle, level);
            }
            if (this.alpha != null) {
                particle.alpha = (float) this.alpha.getValue(particle, level);
            }
            if (this.x != null) {
                particle.x = this.x.getValue(particle, level);
            }
            if (this.y != null) {
                particle.y = this.y.getValue(particle, level);
            }
            if (this.z != null) {
                particle.z = this.z.getValue(particle, level);
            }
            if (this.dx != null) {
                particle.xd = this.dx.getValue(particle, level);
            }
            if (this.dy != null) {
                particle.yd = this.dy.getValue(particle, level);
            }
            if (this.dz != null) {
                particle.zd = this.dz.getValue(particle, level);
            }
            if (this.custom != null) {
                particle.custom = this.custom.getValue(particle, level);
            }
            if (this.removeIf != null) {
                if (this.removeIf.getValue(particle, level) > 0) {
                    particle.remove();
                }
            }
        }

    }

    protected enum LiquidAffinity implements StringRepresentable {
        LIQUIDS, NON_LIQUIDS, ANY;

        private static final Codec<LiquidAffinity> CODEC = StringRepresentable.fromEnum(LiquidAffinity::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    protected enum RotationMode implements StringRepresentable {
        LOOK_AT_XYZ, LOOK_AT_Y,
        LOOK_AT_X, LOOK_AT_Z, LOOK_AT_XZ,
        MOVEMENT_ALIGNED, LOOK_UP, LOOK_WEST, NONE;

        private static final Codec<RotationMode> CODEC = StringRepresentable.fromEnum(RotationMode::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public void setRotation(SingleQuadParticle particle, Quaternionf quaternionf, Camera camera, float partialTicks) {
            switch (this) {
                case NONE -> {
                }
                case LOOK_UP -> {
                    quaternionf.identity(); // Reset rotation
                    quaternionf.rotateX(Mth.HALF_PI);
                }
                case LOOK_WEST -> {
                    quaternionf.identity(); // Reset rotation
                    quaternionf.rotateY(Mth.HALF_PI);
                }
                case LOOK_AT_XYZ ->
                        SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(quaternionf, camera, partialTicks);
                case LOOK_AT_Y ->
                        SingleQuadParticle.FacingCameraMode.LOOKAT_Y.setRotation(quaternionf, camera, partialTicks);
                case LOOK_AT_X -> quaternionf.set(camera.rotation().x, 0, 0.0F, camera.rotation().w);
                case LOOK_AT_Z -> quaternionf.set(0.0F, 0.0F, camera.rotation().z, camera.rotation().w);
                case LOOK_AT_XZ -> quaternionf.set(camera.rotation().x, 0, camera.rotation().z, camera.rotation().w);
                case MOVEMENT_ALIGNED -> {
                    Vec3 dir = new Vec3(particle.xd, particle.yd, particle.zd).normalize();

                    Vec3 cameraLook = new Vec3(camera.getLookVector());
                    Vec3 cross = dir.cross(cameraLook);

                    double pitch = getPitch(dir);
                    double yaw = getYaw(dir);

                    Vector3f dirUp = new Vector3f(0, 1, 0).rotate(quaternionf);
                    float roll = dirUp.angleSigned(cross.toVector3f(), dir.toVector3f());

                    quaternionf.rotateY((float) (-Mth.DEG_TO_RAD * yaw));

                    quaternionf.rotateX((float) (Mth.DEG_TO_RAD * (pitch - 90)));
                    quaternionf.rotateY(-roll - Mth.HALF_PI);
                }
            }
        }

        public boolean hasBackFace() {
            return !(this == LOOK_AT_XYZ || this == LOOK_AT_Y || this == MOVEMENT_ALIGNED);
        }
    }

    // in degrees. Opposite of Vec3.fromRotation
    public static double getPitch(Vec3 vec3) {
        return -Math.toDegrees(Math.asin(vec3.y));
    }

    // in degrees
    public static double getYaw(Vec3 vec3) {
        return Math.toDegrees(Math.atan2(-vec3.x, vec3.z));
    }

    public static final Codec<Optional<ModelResourceLocation>> CUSTOM_MODEL_ONLY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ModelResHelper.MODEL_RES_CODEC.optionalFieldOf("model").forGetter(e -> e)
    ).apply(i, r -> r));
}

