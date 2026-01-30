package net.mehvahdjukaar.polytone.common;

import net.minecraft.util.TriState;

public interface PolyConditionalOverlay {

    void polytone$setCondition(TriState triState);

    TriState polytone$getCondition();

}
