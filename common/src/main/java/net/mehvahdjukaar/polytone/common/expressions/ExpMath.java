package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class ExpMath {

    // Constants
    public static final double PI = Math.PI;
    public static final double TAU = (Math.PI * 2);
    public static final double E = Math.E;
    public static final double PSI = (1 + Math.sqrt(5)) / 2;

    // Basic math
    public static double sin(double x) {
        return Math.sin(x);
    }

    public static double cos(double x) {
        return Math.cos(x);
    }

    public static double tan(double x) {
        return Math.tan(x);
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    public static double abs(double x) {
        return Math.abs(x);
    }

    public static double log(double x) {
        return Math.log(x);
    }

    public static double exp(double x) {
        return Math.exp(x);
    }

    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    public static double floor(double x) {
        return Math.floor(x);
    }

    public static double ceil(double x) {
        return Math.ceil(x);
    }

    public static double round(double x) {
        return Math.round(x);
    }

    public static double fract(double x) {
        return x - Math.floor(x);
    }

    public static double sign(double x) {
        return Math.signum(x);
    }

    public static double radians(double degrees) {
        return Math.toRadians(degrees);
    }

    public static double degrees(double radians) {
        return Math.toDegrees(radians);
    }

    public static double mod(double x, double y) {
        return Mth.positiveModulo(x, y);
    }


    // Min / max / clamp
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    // Helpers
    public static double square(double x) {
        return x * x;
    }

    public static double cube(double x) {
        return x * x * x;
    }

    // Linear interpolation
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double inverseLerp(double a, double b, double v) {
        if (a != b) {
            return (v - a) / (b - a);
        } else {
            return 0;
        }
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    //dist
    public static double distSquare(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static double distSquare(Vec3i p1, Vec3i p2) {
        return distSquare(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public static double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distSquare(x1, y1, x2, y2));
    }

    public static double dist(Vec3i p1, Vec3i p2) {
        return Math.sqrt(distSquare(p1, p2));
    }

    //color
    public static double red(double color) {
        return ARGB.red((int) color) / 255f;
    }

    public static double green(double color) {
        return ARGB.greenFloat((int) color);
    }

    public static double blue(double color) {
        return ARGB.blueFloat((int) color);
    }

    public static double alpha(double color) {
        return ARGB.alphaFloat((int) color);
    }

    public static int color(double r, double g, double b, double a) {
        return ARGB.colorFromFloat((float) a, (float) r, (float) g, (float) b);
    }


    public static int colormap(String colormap, int x, int y, int z, int tint) {
        var c = Polytone.COLORMAPS.get(colormap);
        if (c != null){
            var pos = new BlockPos(x, y, z);
            c.getColor(Blocks.AIR.defaultBlockState(), Minecraft.getInstance().level,
                    pos, tint);
        }
        return 0;
    }

    public static int colormap(String colormap) {
        return colormap(colormap, 0,0,0,0);
    }

}

