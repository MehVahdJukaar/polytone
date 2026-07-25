package net.mehvahdjukaar.polytone.compat;

// Stub - no Iris 26.1 build available yet
public class IrisCompat {

    public static void init() {
        // When an Iris 26.1 build lands, restore the pipeline assignments:
        // LEASH_PIPELINE -> IrisProgram.BLOCK (PARTICLES gives a red/green overlay, TEXTURED/BASIC glow),
        // ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE -> PARTICLES_TRANSLUCENT,
        // ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE -> BLOCK_TRANSLUCENT.
    }


    public static boolean isIrisRenderOn() {
        return false;
    }
}
