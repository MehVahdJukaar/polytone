package net.mehvahdjukaar.polytone.misc.struc;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.misc.ColorUtils;

public record SimpleColor(int r, int g, int b, int a) {

    public SimpleColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public SimpleColor(int color) {
        this((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    public static final Codec<SimpleColor> CODEC = ColorUtils.CODEC.xmap(
            i -> {
                int a = (i >> 24) & 0xFF;
                int r = (i >> 16) & 0xFF;
                int g = (i >> 8) & 0xFF;
                int b = i & 0xFF;
                return new SimpleColor(r, g, b, a);
            },
            color -> (color.a << 24) | (color.r << 16) | (color.g << 8) | color.b
    );

}
