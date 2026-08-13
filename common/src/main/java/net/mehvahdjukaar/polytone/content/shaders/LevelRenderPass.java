package net.mehvahdjukaar.polytone.content.shaders;

// Tells the per-frame shader hooks on LevelRenderer.renderLevel (depth snapshot, shadow map) whether
// the pass they're in is the one that ends up on screen.
//
// Mods that render the world a second time into an off-screen target (Vista's mirrors, portal-style
// mods) call renderLevel themselves, so our TAIL hook fires for those too. Nothing in a secondary
// render consumes what the hook produces, and doing it anyway thrashes the depth snapshot between the
// window and canvas sizes and leaves the shadow reuse cache holding a foreign camera's matrix.
//
// A secondary render bypasses GameRenderer.renderLevel; only vanilla's own frame goes through it. The
// depth counter then picks the outermost renderLevel, for mods that nest theirs inside the main pass.
public final class LevelRenderPass {

    private static boolean inVanillaFrame = false;
    private static int depth = 0;

    public static void startVanillaFrame() {
        inVanillaFrame = true;
        // renderLevel's TAIL doesn't run if the pass throws, so re-anchor rather than trusting the pops.
        depth = 0;
    }

    public static void endVanillaFrame() {
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
