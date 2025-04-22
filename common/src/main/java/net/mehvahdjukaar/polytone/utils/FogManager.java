package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FogManager {

    private static final FogState ONE = new FogState(1, 1);

    private static final FogStateMutable lastBiomeFog = new FogStateMutable();
    private static final FogStateMutable lastLiquidFog = new FogStateMutable();


    @Nullable
    public static FogState modifyBiomeFog(float originalNearPlane, float originalFarPlane) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return null;

        Level level = player.level();
        //dont modify if a mob effect that modifies fog is active
        if (FogRenderer.getPriorityFogFunction(player, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))
                != null) return null;
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        var fogMod = Polytone.BIOME_MODIFIERS.getFogModifier(biome.value());
        FogParam fogRadius = null;
        FogParam fogFade = null;
        if (fogMod != null) {
            fogRadius = fogMod.fogRadius().orElse(null);
            fogFade = fogMod.fogFade().orElse(null);
        }
        return modifyFogParameters(level, originalNearPlane, originalFarPlane, fogRadius, fogFade, lastBiomeFog);
    }

    @Nullable
    public static FogState modifyFluidFog(float originalNearPlane, float originalFarPlane,
                                          @Nullable FogParam fogRadius, @Nullable FogParam fogFade) {
        return modifyFogParameters(Minecraft.getInstance().level, originalNearPlane, originalFarPlane, fogRadius, fogFade, lastLiquidFog);
    }


    @Nullable
    private static FogState modifyFogParameters(@Nullable Level level, float originalNearPlane, float originalFarPlane,
                                                @Nullable FogParam fogRadius, @Nullable FogParam fogFade,
                                                FogStateMutable old) {
        if (level == null) return null;
        FogState params = null;
        if (fogRadius != null || fogFade != null) {
            params = new FogState(
                    fogRadius != null ? fogRadius.get(level) : 1,
                    fogFade != null ? fogFade.get(level) : 1
            );
        }


        //interpolation
        if (params == null && (Mth.abs(old.distanceMult - 1) > 0.02f || Mth.abs(old.endMult - 1) > 0.02f)) {
            params = ONE;
        }
        if (params != null) {
            float deltaTime = ClientFrameTicker.getDeltaTime(); // Get time since last frame
            float interpolationFactor = deltaTime * 0.1f;

            // Interpolate towards the fogScalars values
            old.distanceMult = Mth.lerp(interpolationFactor, old.distanceMult, params.start);
            old.endMult = Mth.lerp(interpolationFactor, old.endMult, params.end);
            //fogEvent.scaleNearPlaneDistance(1);
            float distance = originalFarPlane - originalNearPlane;

            return new FogState((originalFarPlane - distance * old.distanceMult) * old.endMult, originalFarPlane * old.endMult);
        }

        return null;
    }


    public record FogState(
            float start,
            float end) {
    }

    private static class FogStateMutable {
        private float distanceMult = 1;
        private float endMult = 1;
    }


    public interface FogParam {
        float get(Level level);

        Codec<FogParam> SIMPLE_CODEC = Codec.FLOAT.xmap(f -> (l) -> f, fogParam -> fogParam.get(null));
        Codec<FogParam> CODEC = Codec.withAlternative(
                Codec.withAlternative(SIMPLE_CODEC,
                        Codec.simpleMap(Weather.CODEC, SIMPLE_CODEC, StringRepresentable.keys(Weather.values()))
                                .xmap(FogMap::new, FogMap::map).codec()
                ),
                BlockContextExpression.CODEC.xmap(
                        FogExpression::new,
                        fogMap -> fogMap.map
                )
        );
    }

    public record FogExpression(BlockContextExpression map) implements FogParam {

        @Override
        public float get(Level level) {
            BlockPos pos = ClientFrameTicker.getCameraPos();
            return (float) map.getValue(level, pos, Blocks.AIR.defaultBlockState());
        }
    }

    public record FogMap(Map<Weather, FogParam> map) implements FogParam {

        @Override
        public float get(Level level) {
            Weather w = Weather.get(level);
            return map.getOrDefault(w, (l) -> 1).get(level);
        }
    }

}
