package net.mehvahdjukaar.polytone.content.common.expressions;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        return Mth.clamp(val, min, max);
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

    //color (no ARGB class on 1.21.1 - inline bit math)
    public static double red(double color) {
        return ((((int) color) >> 16) & 0xFF) / 255f;
    }

    public static double green(double color) {
        return ((((int) color) >> 8) & 0xFF) / 255f;
    }

    public static double blue(double color) {
        return (((int) color) & 0xFF) / 255f;
    }

    public static double alpha(double color) {
        return ((((int) color) >>> 24) & 0xFF) / 255f;
    }

    public static int color(double r, double g, double b, double a) {
        int ai = Mth.clamp((int) (a * 255f), 0, 255);
        int ri = Mth.clamp((int) (r * 255f), 0, 255);
        int gi = Mth.clamp((int) (g * 255f), 0, 255);
        int bi = Mth.clamp((int) (b * 255f), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    public static int colormap(String colormap, double x, double y, double z) {
        return colormap(colormap, x, y, z, 0);
    }

    public static int colormap(String colormap, double x, double y, double z, double tint) {
        var c = Polytone.COLORMAPS.get(colormap);
        if (c == null) {
            Polytone.LOGGER.warn("Colormap '{}' not found!", colormap);
            return 0;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) return 0;
        BlockPos pos = BlockPos.containing(x, y, z);
        // unloaded chunk would hand back an air fallback, which is not what the sampler wants
        BlockState state = level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        return c.getColor(state, level, pos, (int) tint);
    }


    //general stuff. shouldnt be here really...

    public static int colormap(String colormap) {
        return colormap(colormap, 0, 0, 0, 0);
    }

    public static Object config(String key) {
        try {
            ResourceLocation configKey = ResourceLocation.parse(key);
            return Polytone.CONFIGS.getValue(configKey);
        } catch (Exception e) {
            Polytone.LOGGER.error("Could not parse Identifier '{}'", key, e);
            return 0;
        }
    }

    public static boolean modOn(String mod) {
        return PlatStuff.isModLoaded(mod);
    }

    public static boolean modOn(String mod, String versionRange) {
        // Stub - 1.21.1 PlatStuff doesn't expose mod version; treat as no version match
        return PlatStuff.isModLoaded(mod);
    }

    public static double dateYear() {
        return LocalDate.now().getYear();
    }

    public static double dateMonth() {
        return LocalDate.now().getMonthValue();
    }

    public static double dateDay() {
        return LocalDate.now().getDayOfMonth();
    }

    public static double dateHour() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour();
    }

    public static double dateMinute() {
        LocalDateTime now = LocalDateTime.now();
        return now.getMinute(); // 0-59
    }

    public static long dateTime() {
        return Instant.now().getEpochSecond();
    }

    public static String modLoader() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return "Fabric";
        } catch (ClassNotFoundException e) {
            return "Neoforge";
        }
    }

    public static int dateDayOfYear() {
        return LocalDate.now().getDayOfYear();
    }

    public static String osName() {
        return System.getProperty("os.name");
    }

}
