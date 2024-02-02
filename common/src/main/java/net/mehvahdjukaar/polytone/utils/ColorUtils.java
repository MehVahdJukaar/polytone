package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.accessor.BiomeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Dynamic;

import java.util.Locale;

//ML class
public class ColorUtils {

    //utility codec that serializes either a string or an integer
    public static final Codec<Integer> CODEC = Codec.either(Codec.intRange(0, 0xffffffff),
            Codec.STRING.flatXmap(ColorUtils::isValidStringOrError, s->isValidStringOrError(s)
                    .map(ColorUtils::formatString))).xmap(
            either -> either.map(integer -> integer, s -> Integer.parseUnsignedInt(s, 16)),
            integer -> Either.right("#" + String.format("%08X", integer))
    );

    private static String formatString(String s){
        return "#"+ s.toUpperCase(Locale.ROOT);
    }

    public static DataResult<String> isValidStringOrError(String s) {
        String st = s;
        if (s.startsWith("0x")) {
            st = s.substring(2);
        } else if (s.startsWith("#")) {
            st = s.substring(1);
        }

        // Enforce the maximum length of eight characters (including prefix)
        if (st.length() > 8) {
            return DataResult.error(()-> "Invalid color format. Hex value must have up to 8 characters.");
        }

        try {
            int parsedValue = Integer.parseUnsignedInt(st, 16);
            return DataResult.success(st);
        } catch (NumberFormatException e) {
            return DataResult.error(()-> "Invalid color format. Must be in hex format (0xff00ff00, #ff00ff00, ff00ff00) or integer value");
        }
    }

    public static boolean isValidString(String s) {
        return isValidStringOrError(s).result().isPresent();
    }


    public static int pack(float... components) {
        int n = (int) (components[0] * 255.0F) << 16;
        int o = (int) (components[1] * 255.0F) << 8;
        int p = (int) (components[2] * 255.0F);
        return (n & 0xFF0000) | (o & 0xFF00) | (p & 0xFF);
    }

    public static float[] unpack(int value) {
        int n = (value & 16711680) >> 16;
        int o = (value & '\uff00') >> 8;
        int p = (value & 255);
        return new float[]{n / 255.0F, o / 255.0F, p / 255.0F};
    }


    public static Biome.ClimateSettings getClimateSettings(Level level, BlockPos pos){
        return getClimateSettings(level.getBiome(pos).value());
    }
    public static Biome.ClimateSettings getClimateSettings(Biome biome){
        return ((BiomeAccessor) (Object)biome).getClimateSettings();
    }


}
