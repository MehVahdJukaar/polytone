package net.mehvahdjukaar.polytone.content.shaders;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IProgramWithModifiers {

    @Nullable
    EnabledModifiers polytone$getModifiers();

    void polytone$setModifiers(EnabledModifiers modifiers);

    record EnabledModifiers(List<IShaderModifier> modifiers, int generation) {
    }
}
