package net.mehvahdjukaar.polytone.content.shaders;

public final class LevelRenderPassTrack {

    private static boolean inVanillaFrame = false;
    private static int depth = 0;

    public static void onStartRenderLevel() {
        inVanillaFrame = true;
        // renderLevel's TAIL doesn't run if the pass throws, so re-anchor rather than trusting the pops.
        depth = 0;
    }

    public static void onEndRenderLevel() {
        inVanillaFrame = false;
    }

    public static void push() {
        depth++;
    }

    public static boolean popAndWasMain() {
        depth = Math.max(0, depth - 1);
        return inVanillaFrame && depth == 0;
    }
}
