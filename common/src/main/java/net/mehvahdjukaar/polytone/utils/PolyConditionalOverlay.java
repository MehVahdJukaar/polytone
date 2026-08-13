package net.mehvahdjukaar.polytone.utils;

// duck interface on OverlayMetadataSection.OverlayEntry, so an overlay entry can carry a polytone
// condition evaluated against the config state at pack-read time
public interface PolyConditionalOverlay {

    void polytone$setCondition(TriState triState);

    TriState polytone$getCondition();

}
