package net.mehvahdjukaar.moonlight.api.client.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;

public class ParticleUtil {

    //call with packet

    public static void spawnParticleOnBlockShape(Level level, BlockPos pos, ParticleOptions particleOptions,
                                                 UniformInt uniformInt, float maxSpeed) {
        spawnParticleOnBoundingBox(level.getBlockState(pos).getShape(level, pos).bounds().move(pos), level,
                particleOptions, uniformInt, maxSpeed);
    }

    public static void spawnParticleOnBoundingBox(AABB bb, Level level, ParticleOptions particleOptions,
                                                  UniformInt uniformInt, float maxSpeed) {

        RandomSource random = level.random;
        float offset = 0.1f;
        Vec3 blockCenter = new Vec3(bb.minX - 0.5 + (bb.maxX - bb.minX) / 2f, bb.minY - 0.5 + (bb.maxY - bb.minY) / 2f, bb.minZ - 0.5 + (bb.maxZ - bb.minZ) / 2f);
        bb = bb.move(-blockCenter.x, -blockCenter.y, -blockCenter.z);
        //north
        int i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            if (x > bb.minX && x < bb.maxX && y > bb.minY && y < bb.maxY) {
                double dx = maxSpeed * level.random.nextDouble();
                double dy = maxSpeed * level.random.nextDouble();
                double dz = 0;
                level.addParticle(particleOptions, blockCenter.x + x, blockCenter.y + y, blockCenter.z + bb.minZ - offset, dx, dy, dz);
            }
        }
        //south
        i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            if (x > bb.minX && x < bb.maxX && y > bb.minY && y < bb.maxY) {
                double dx = maxSpeed * level.random.nextDouble();
                double dy = maxSpeed * level.random.nextDouble();
                double dz = 0;
                level.addParticle(particleOptions, blockCenter.x() + x, blockCenter.y() + y, blockCenter.z() + bb.maxZ + offset, dx, dy, dz);
            }
        }
        //west
        i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double z = random.nextDouble();
            double y = random.nextDouble();
            if (z > bb.minZ && z < bb.maxZ && y > bb.minY && y < bb.maxY) {
                double dx = 0;
                double dy = maxSpeed * level.random.nextDouble();
                double dz = maxSpeed * level.random.nextDouble();
                level.addParticle(particleOptions, blockCenter.x() + bb.minX - offset, blockCenter.y() + y, blockCenter.z() + z, dx, dy, dz);
            }
        }
        //east
        i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double z = random.nextDouble();
            double y = random.nextDouble();
            if (z > bb.minZ && z < bb.maxZ && y > bb.minY && y < bb.maxY) {
                double dx = 0;
                double dy = maxSpeed * level.random.nextDouble();
                double dz = maxSpeed * level.random.nextDouble();
                level.addParticle(particleOptions, blockCenter.x() + bb.maxX + offset, blockCenter.y() + y, blockCenter.z() + z, dx, dy, dz);
            }
        }
        //down
        i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double x = random.nextDouble();
            double z = random.nextDouble();
            if (x > bb.minX && x < bb.maxX && z > bb.minZ && z < bb.maxZ) {
                double dx = maxSpeed * level.random.nextDouble();
                double dy = 0;
                double dz = maxSpeed * level.random.nextDouble();
                level.addParticle(particleOptions, blockCenter.x() + x, blockCenter.y() + bb.minY - offset, blockCenter.z() + z, dx, dy, dz);
            }
        }
        //up
        i = uniformInt.sample(random);
        for (int j = 0; j < i; ++j) {
            double x = random.nextDouble();
            double z = random.nextDouble();
            if (x > bb.minX && x < bb.maxX && z > bb.minZ && z < bb.maxZ) {
                double dx = maxSpeed * level.random.nextDouble();
                double dy = 0;
                double dz = maxSpeed * level.random.nextDouble();
                level.addParticle(particleOptions, blockCenter.x() + x, blockCenter.y() + bb.maxY + offset, blockCenter.z() + z, dx, dy, dz);
            }
        }
    }


    public static void spawnParticlesOnBlockFaces(Level level, BlockPos pos, ParticleOptions particleOptions,
                                                  UniformInt uniformInt, float minSpeed, float maxSpeed, boolean perpendicular) {
        for (Direction direction : Direction.values()) {
            int i = uniformInt.sample(level.random);

            for (int j = 0; j < i; ++j) {
                spawnParticleOnFace(level, pos, direction, particleOptions, minSpeed, maxSpeed, perpendicular);
            }
        }
    }

    public static void spawnParticleOnFace(Level level, BlockPos pos, Direction direction, ParticleOptions particleOptions,
                                           float minSpeed, float maxSpeed, boolean perpendicular) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d0 = vec3.x + (i == 0 ? Mth.nextDouble(level.random, -0.5D, 0.5D) : i * 0.6D);
        double d1 = vec3.y + (j == 0 ? Mth.nextDouble(level.random, -0.5D, 0.5D) : j * 0.6D);
        double d2 = vec3.z + (k == 0 ? Mth.nextDouble(level.random, -0.5D, 0.5D) : k * 0.6D);
        double dx;
        double dy;
        double dz;
        if (perpendicular) {
            dx = i * Mth.randomBetween(level.random, minSpeed, maxSpeed);
            dy = j * Mth.randomBetween(level.random, minSpeed, maxSpeed);
            dz = k * Mth.randomBetween(level.random, minSpeed, maxSpeed);
        } else {
            float d = maxSpeed - minSpeed;

            dx = (i == 0) ? (minSpeed + d * level.random.nextDouble()) : 0.0D;
            dy = (j == 0) ? (minSpeed + d * level.random.nextDouble()) : 0.0D;
            dz = (k == 0) ? (minSpeed + d * level.random.nextDouble()) : 0.0D;
        }
        level.addParticle(particleOptions, d0, d1, d2, dx, dy, dz);
    }


    public static void spawnBreakParticles(VoxelShape shape, BlockPos pPos, BlockState pState, Level level) {

        var particleEngine = Minecraft.getInstance().particleEngine;

        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double d1 = Math.min(1.0D, x1 - x0);
            double d2 = Math.min(1.0D, y1 - y0);
            double d3 = Math.min(1.0D, z1 - z0);
            int i = Math.max(2, Mth.ceil(d1 / 0.25D));
            int j = Math.max(2, Mth.ceil(d2 / 0.25D));
            int k = Math.max(2, Mth.ceil(d3 / 0.25D));

            for (int l = 0; l < i; ++l) {
                for (int i1 = 0; i1 < j; ++i1) {
                    for (int j1 = 0; j1 < k; ++j1) {
                        double d4 = (l + 0.5D) / i;
                        double d5 = (i1 + 0.5D) / j;
                        double d6 = (j1 + 0.5D) / k;
                        double d7 = d4 * d1 + x0;
                        double d8 = d5 * d2 + y0;
                        double d9 = d6 * d3 + z0;
                        particleEngine.add(new TerrainParticle((ClientLevel) level, pPos.getX() + d7, pPos.getY() + d8,
                                pPos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, pState, pPos));
                    }
                }
            }
        });
    }

    @ApiStatus.Internal
    public static Supplier<ShaderInstance> particleShader = GameRenderer::getParticleShader;

    public static final ParticleRenderType ADDITIVE_TRANSLUCENCY_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.setShader(particleShader);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public String toString() {
            return "PARTICLE_SHEET_ADDITIVE_TRANSLUCENT";
        }
    };
}
