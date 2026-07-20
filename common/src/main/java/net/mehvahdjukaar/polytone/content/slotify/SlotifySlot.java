package net.mehvahdjukaar.polytone.content.slotify;

/**
 * Duck interface on vanilla {@link net.minecraft.world.inventory.Slot} that remembers a slot's
 * pristine (unmodified) position, so slot offsets can be re-applied idempotently. Needed by the
 * live editor preview, which pushes a modifier onto an already-built menu repeatedly.
 */
public interface SlotifySlot {

    // Records the current position as the base the first time it is called, then never again.
    void polytone$captureBase();

    // Restores the slot to its captured base position (no-op if never captured).
    void polytone$resetToBase();
}
