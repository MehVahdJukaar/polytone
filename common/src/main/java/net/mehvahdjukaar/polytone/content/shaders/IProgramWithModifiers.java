package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IProgramWithModifiers {

    @Nullable
    EnabledModifiers polytone$getModifiers();

    void polytone$setModifiers(EnabledModifiers modifiers);

    record EnabledModifiers(List<IShaderInputPatch> modifiers, int generation) {
    }
}
