package net.mehvahdjukaar.polytone.bedrock.convert;

// Bedrock states everything in seconds, meters and degrees and evaluates per frame; we tick 20 times a second
// and rotate in radians. Every conversion goes through here so the factors live in one place.
public class BedrockUnits {

    public static final int TICKS_PER_SECOND = 20;

    public static double secondsToTicks(double seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    // Velocities: meters per second to blocks per tick
    public static double perSecondToPerTick(double value) {
        return value / TICKS_PER_SECOND;
    }

    // Accelerations: meters per second squared to blocks per tick squared
    public static double perSecondSquaredToPerTickSquared(double value) {
        return value / ((double) TICKS_PER_SECOND * TICKS_PER_SECOND);
    }

    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    // Spin rates: degrees per second to radians per tick
    public static double degreesPerSecondToRadiansPerTick(double degrees) {
        return perSecondToPerTick(degreesToRadians(degrees));
    }

    // Bedrock drag is a coefficient applied per second as v -= v * drag * dt. Per tick that leaves v * (1 -
    // drag/20), which is the multiplier our ticker needs. Clamped at 0 because a drag above 20 would otherwise
    // flip the velocity sign every tick instead of stopping it.
    public static double dragToPerTickMultiplier(double dragCoefficient) {
        return Math.max(0, 1 - dragCoefficient / TICKS_PER_SECOND);
    }
}
