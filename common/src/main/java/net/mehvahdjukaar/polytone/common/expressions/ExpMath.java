package net.mehvahdjukaar.polytone.common.expressions;

import net.minecraft.util.Mth;

public class ExpMath {

    // Constants
    public static final double PI =  Math.PI;
    public static final double TAU =  (Math.PI * 2);
    public static final double E =  Math.E;
    public static final double PSI =  (1 + Math.sqrt(5)) / 2;

    // Basic math
    public static double sin(double x) { return Math.sin(x); }
    public static double cos(double x) { return Math.cos(x); }
    public static double tan(double x) { return  Math.tan(x); }
    public static double atan2(double y, double x) { return  Math.atan2(y, x); }
    public static double sqrt(double x) { return Math.sqrt(x); }
    public static double abs(double x) { return Math.abs(x); }
    public static double log(double x) { return  Math.log(x); }
    public static double exp(double x) { return  Math.exp(x); }
    public static double pow(double a, double b) { return  Math.pow(a, b); }
    public static double floor(double x) { return Math.floor(x); }
    public static double ceil(double x) { return Math.ceil(x); }
    public static double round(double x) { return Math.round(x); }
    public static double fract(double x) { return x - Math.floor(x); }
    public static double sign(double x) { return Math.signum(x); }
    public static double radians(double degrees) { return Math.toRadians(degrees); }
    public static double degrees(double radians) { return Math.toDegrees(radians); }
    public static double mod(double x, double y) { return Mth.positiveModulo(x, y); }


    // Min / max / clamp
    public static double min(double a, double b) { return Math.min(a, b); }
    public static double max(double a, double b) { return Math.max(a, b); }
    public static double clamp(double val, double min, double max) { return Math.max(min, Math.min(max, val)); }

    // Helpers
    public static double square(double x) { return x * x; }
    public static double cube(double x) { return x * x * x; }

    // Linear interpolation
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}

