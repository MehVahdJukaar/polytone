package net.mehvahdjukaar.polytone.bedrock.convert;

// Bedrock is seconds, meters and degrees per frame. We are ticks, blocks and radians at 20 tps.
public class BedrockUnits {

    public static final int TICKS_PER_SECOND = 20;

    public static double secondsToTicks(double seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    public static double perSecondToPerTick(double value) {
        return value / TICKS_PER_SECOND;
    }

    public static double perSecondSquaredToPerTickSquared(double value) {
        return value / ((double) TICKS_PER_SECOND * TICKS_PER_SECOND);
    }

    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    public static double degreesPerSecondToRadiansPerTick(double degrees) {
        return perSecondToPerTick(degreesToRadians(degrees));
    }

    // Bedrock drag applies per second as v -= v * drag * dt, so per tick it is v * (1 - drag/20).
    // Clamped at 0: a drag above 20 would flip the velocity sign every tick instead of stopping it.
    public static double dragToPerTickMultiplier(double dragCoefficient) {
        return Math.max(0, 1 - dragCoefficient / TICKS_PER_SECOND);
    }
}
