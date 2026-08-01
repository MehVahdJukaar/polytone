package net.mehvahdjukaar.polytone.content.shaders;

// mods like Vista or portal mods call LevelRenderer.render again for their own cameras, skipping GameRenderer.renderLevel
public final class LevelRenderPassTrack {

    private static boolean inVanillaFrame = false;
    private static int depth = 0;

    public static void onStartRenderLevel() {
        inVanillaFrame = true;
        // renderLevel's RETURN doesn't run if the pass throws, so re-anchor rather than trusting the pops.
        depth = 0;
    }

    public static void onEndRenderLevel() {
        inVanillaFrame = false;
    }

    public static boolean pushAndIsMain() {
        boolean isMain = inVanillaFrame && depth == 0;
        depth++;
        return isMain;
    }

    public static void pop() {
        depth = Math.max(0, depth - 1);
    }
}
