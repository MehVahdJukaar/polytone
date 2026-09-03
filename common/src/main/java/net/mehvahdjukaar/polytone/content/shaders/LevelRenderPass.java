package net.mehvahdjukaar.polytone.content.shaders;

// Tells the per-frame shader hooks sitting on LevelRenderer.renderLevel (depth snapshot, shadow map)
// whether the pass they're in is the one that ends up on screen.
//
// Mods that render the world a second time into an off-screen target call LevelRenderer.renderLevel
// themselves: Vista (mirrors, TV feeds, and their recursive nesting) flushes its queue from a
// top-level frame hook after the frame is composited, portal-style mods nest theirs inside the main
// pass. Our TAIL hook fires for every one of them. Nothing in a secondary render consumes what the
// hook produces - the post chains run once per frame from GameRenderer.render, for the main camera,
// and InShadow is a post-chain sampler - so the work there is pure waste. It is also actively harmful:
// the depth snapshot gets reallocated back and forth between the window and canvas sizes, and the
// shadow reuse cache ends up holding a foreign camera's matrix, which the next frame then translates
// by the distance between the two cameras.
//
// The discriminator is that a secondary render bypasses GameRenderer.renderLevel - only vanilla's own
// frame goes through it. The depth counter then picks the outermost renderLevel, for mods that nest
// theirs inside the main pass rather than running it after.
public final class LevelRenderPass {

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
