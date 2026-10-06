package net.mehvahdjukaar.polytone.content.shaders;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface IProgramWithModifiers {

    @Nullable
    EnabledModifiers polytone$getModifiers();

    void polytone$setModifiers(EnabledModifiers modifiers);

    Set<String> polytone$getUniformNames();

    //-1 when the program isnt a GL one
    int polytone$getProgramId();

    record EnabledModifiers(List<IShaderModifier> modifiers, int generation) {
    }
}
