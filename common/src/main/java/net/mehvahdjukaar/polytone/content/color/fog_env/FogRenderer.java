package net.mehvahdjukaar.polytone.content.color.fog_env;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class FogRenderer   {
    public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
    public static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.<FogEnvironment>newArrayList(
            new LavaFogEnvironment(),
            new PowderedSnowFogEnvironment(),
            new BlindnessFogEnvironment(),
            new DarknessFogEnvironment(),
            new WaterFogEnvironment(),
            new AtmosphericFogEnvironment()
    );

    private Vector4f computeFogColor(Camera camera, float partialTicks, ClientLevel clientLevel, int renderDistance, float worldDarken) {
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogEnvironment firstFogEnvWithColor = null;
        FogEnvironment firstFogEnvWithDarkness = null;

        for (FogEnvironment f : FOG_ENVIRONMENTS) {
            if (f.isApplicable(fogType, entity)) {
                if (firstFogEnvWithColor == null && f.providesColor()) {
                    firstFogEnvWithColor = f;
                }

                if (firstFogEnvWithDarkness == null && f.modifiesDarkness()) {
                    firstFogEnvWithDarkness = f;
                }
            }
        }

        if (firstFogEnvWithColor == null) {
            throw new IllegalStateException("No color source environment found");
        } else {
            int baseColor = firstFogEnvWithColor.getBaseColor(clientLevel, camera, renderDistance, partialTicks);
            float h = clientLevel.getLevelData().voidDarknessOnsetRange();
            float k = Mth.clamp((h + clientLevel.getMinY() - (float)camera.position().y) / h, 0.0F, 1.0F);
            if (firstFogEnvWithDarkness != null) {
                LivingEntity livingEntity = (LivingEntity)entity;
                k = firstFogEnvWithDarkness.getModifiedDarkness(livingEntity, k, partialTicks);
            }

            float l = ARGB.redFloat(baseColor);
            float m = ARGB.greenFloat(baseColor);
            float n = ARGB.blueFloat(baseColor);
            if (k > 0.0F && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW) {
                float o = Mth.square(1.0F - k);
                l *= o;
                m *= o;
                n *= o;
            }
            if (worldDarken > 0.0F) {
                l = Mth.lerp(worldDarken, l, l * 0.7F);
                m = Mth.lerp(worldDarken, m, m * 0.6F);
                n = Mth.lerp(worldDarken, n, n * 0.6F);
            }

            float o;
            if (fogType == FogType.WATER) {
                if (entity instanceof LocalPlayer) {
                    o = ((LocalPlayer)entity).getWaterVision();
                } else {
                    o = 1.0F;
                }
            } else if (entity instanceof LivingEntity livingEntity2 && livingEntity2.hasEffect(MobEffects.NIGHT_VISION) && !livingEntity2.hasEffect(MobEffects.DARKNESS)
            )
            {
                o = GameRenderer.getNightVisionScale(livingEntity2, partialTicks);
            } else {
                o = 0.0F;
            }

            if (l != 0.0F && m != 0.0F && n != 0.0F) {
                float p = 1.0F / Math.max(l, Math.max(m, n));
                l = Mth.lerp(o, l, l * p);
                m = Mth.lerp(o, m, m * p);
                n = Mth.lerp(o, n, n * p);
            }

            return new Vector4f(l, m, n, 1.0F);
        }
    }

    //returns clear color
    public Vector4f setupFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                             float darkenWorldAmount, ClientLevel clientLevel) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        Vector4f fogColor = this.computeFogColor(camera, partialTicks, clientLevel, renderDistance, darkenWorldAmount);
        float blocksRenderDistance = renderDistance * 16;
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogData fogData = new FogData();

        for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
            if (fogEnvironment.isApplicable(fogType, entity)) {
                fogEnvironment.setupFog(fogData, camera, clientLevel, blocksRenderDistance, deltaTracker);
                break;
            }
        }

        float distanceFrom4To64Scaled10 = Mth.clamp(blocksRenderDistance / 10.0F, 4.0F, 64.0F);
        fogData.renderDistanceStart = blocksRenderDistance - distanceFrom4To64Scaled10;
        fogData.renderDistanceEnd = blocksRenderDistance;

        return fogColor;
    }

    private FogType getFogType(Camera camera) {
        FogType fogType = camera.getFluidInCamera();
        return fogType == FogType.NONE ? FogType.ATMOSPHERIC : fogType;
    }

    private void updateBuffer(ByteBuffer byteBuffer, int i, Vector4f vector4f, float f, float g, float h, float j, float k, float l) {
        byteBuffer.position(i);
        Std140Builder.intoBuffer(byteBuffer).putVec4(vector4f).putFloat(f).putFloat(g).putFloat(h).putFloat(j).putFloat(k).putFloat(l);
    }

    @Environment(EnvType.CLIENT)
    public static enum FogMode {
        NONE,
        WORLD;
    }
}
