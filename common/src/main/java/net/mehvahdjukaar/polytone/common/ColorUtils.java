package net.mehvahdjukaar.polytone.common;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.mixins.accessor.BiomeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;

//ML class
public class ColorUtils {

    //Known uses: Gui text (ARGB), VertexConsumer (ABGR), BiomeColors (ARGB) unused alpha.
    //automatically fills in alpha if not provided
    //ARGB or ABGR
    public static final Codec<Integer> COLOR =
            Codec.either(Codec.INT, Codec.STRING.flatXmap(
                    s -> parseHex(s, true),
                    s -> parseHex(s, true)
            )).xmap(
                    e -> e.map(i -> i, s -> Integer.parseUnsignedInt(s, 16)),
                    i -> Either.right("#" + String.format("%08X", i))
            );

    /* -------------------- HEX PARSING -------------------- */

    private static DataResult<String> parseHex(String s, boolean fillAlphaFirst) {
        String st = s;

        if (s.startsWith("0x")) {
            st = s.substring(2);
        } else if (s.startsWith("#")) {
            st = s.substring(1);
        }

        if (st.length() != 6 && st.length() != 8) {
            return DataResult.error(() ->
                    "Invalid color format. Must be 6 (RRGGBB) or 8 (AARRGGBB) hex digits."
            );
        }

        try {
            Integer.parseUnsignedInt(st, 16);

            // ARGB codec: inject full alpha if missing
            if (fillAlphaFirst && st.length() == 6) {
                st = "FF" + st;
            }

            return DataResult.success(st.toUpperCase(Locale.ROOT));
        } catch (NumberFormatException e) {
            return DataResult.error(() ->
                    "Invalid color format. Must be hexadecimal."
            );
        }
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
