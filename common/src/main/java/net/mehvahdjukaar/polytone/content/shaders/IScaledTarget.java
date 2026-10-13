package net.mehvahdjukaar.polytone.content.shaders;

import org.jspecify.annotations.Nullable;

// "scale" on a post_effect internal target, multiplying its width/height when given, else the screen size
public interface IScaledTarget {

    // never below 1, not clamped above since vanilla allows targets bigger than the screen
    static int resolve(float scale, int size) {
        return Math.max(1, Math.round(size * scale));
    }

    @Nullable
    Float polytone$getScale();

    void polytone$setScale(@Nullable Float scale);
}
