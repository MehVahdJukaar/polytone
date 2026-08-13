package net.mehvahdjukaar.polytone.content.slotify;

// Remembers a slot's pristine position so slot offsets can be re-applied idempotently, which the
// live editor preview needs since it pushes a modifier onto an already built menu repeatedly.
public interface SlotifySlot {

    void polytone$captureBase();

    void polytone$resetToBase();
}
