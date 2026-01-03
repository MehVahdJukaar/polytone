package net.mehvahdjukaar.polytone.common.expressions.proxies;

public class ExpMath {

    // Constants
    public static final double PI = Math.PI;
    public static final double TAU = Math.PI * 2;
    public static final double E = Math.E;

    // Basic math
    public static double sin(double x) { return Math.sin(x); }
    public static double cos(double x) { return Math.cos(x); }
    public static double tan(double x) { return Math.tan(x); }
    public static double sqrt(double x) { return Math.sqrt(x); }
    public static double abs(double x) { return Math.abs(x); }

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

