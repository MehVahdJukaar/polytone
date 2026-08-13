package net.mehvahdjukaar.polytone.content.slotify;

// Remembers a slot's pristine position so slot offsets can be re-applied idempotently, which the
// live editor preview needs since it pushes a modifier onto an already built menu repeatedly.
public interface SlotifySlot {

    // Records the current position as the base the first time it is called, then never again.
    void polytone$captureBase();

    // Restores the slot to its captured base position (no-op if never captured).
    void polytone$resetToBase();
}
