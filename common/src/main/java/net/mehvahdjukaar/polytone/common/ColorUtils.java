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

    //utility codecs that serializes either a string or an integer

    //RGBA
    public static final Codec<Integer> RGBA_COLOR = Codec.either(Codec.INT,
            Codec.STRING.flatXmap(
                    s -> parseHex(s, false),
                    s -> parseHex(s, false)
            )).xmap(
            either -> either.map(integer -> integer, s -> Integer.parseUnsignedInt(s, 16)),
            integer -> Either.right("#" + String.format("%08X", integer))
    );

    //Known uses: Gui text (ARGB), VertexConsumer (ABGR), BiomeColors (ARGB)
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
        // Enforce the maximum length of eight characters (including prefix)
        if (st.length() > 8) {
            return DataResult.error(()-> "Invalid color format. Hex value must have up to 8 characters.");
        }
        try {
            Integer.parseUnsignedInt(st, 16);

            // ARGB codec: inject full alpha if missing
            if (fillAlphaFirst && st.length() == 6) {
                st = "FF" + st;
            }

            return DataResult.success(st.toUpperCase(Locale.ROOT));
        } catch (NumberFormatException e) {
            //No int allowed unless in primitive type
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


    public static int blendColor(int original, int blend){
            // unpack colors to float arrays [r, g, b] in 0..1
            float[] orig = unpack(original);
            float[] b = unpack(blend);

            // average each channel
            float r = (orig[0] + b[0]) / 2f;
            float g = (orig[1] + b[1]) / 2f;
            float bl = (orig[2] + b[2]) / 2f;

            // pack back to integer
            return pack(r, g, bl);
    }

}
