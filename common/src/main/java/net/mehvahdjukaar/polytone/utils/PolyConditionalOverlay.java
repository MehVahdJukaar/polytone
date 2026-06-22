package net.mehvahdjukaar.polytone.utils;

/**
 * Duck interface mixed into {@code OverlayMetadataSection.OverlayEntry} so each overlay entry can
 * carry a polytone condition evaluated against the config state at pack-read time.
 */
public interface PolyConditionalOverlay {

    void polytone$setCondition(TriState triState);

    TriState polytone$getCondition();

}
