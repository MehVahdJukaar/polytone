package net.mehvahdjukaar.polytone.content.colormap;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

//mega hack to pass more xontext down colormap call chain
public class ColormapContext {

    private @Nullable BlockAndTintGetter level;
    private @Nullable BlockState state;
    private @Nullable BlockPos pos;
    private int tintIndex;
    private @Nullable BlockColor original;
    private boolean computingOriginal;
    private boolean hasOriginalColor;
    private int originalColor;

    public void startBlock(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable BlockPos pos,
                           int tintIndex, @Nullable BlockColor original) {
        this.level = level;
        this.state = state;
        this.pos = pos;
        this.tintIndex = tintIndex;
        this.original = original;
        this.hasOriginalColor = false;
    }

    public void endBlock() {
        this.state = null;
        this.pos = null;
        this.original = null;
    }

    public void setLevel(@Nullable BlockAndTintGetter level) {
        this.level = level;
    }

    public @Nullable BlockAndTintGetter level() {
        return level;
    }

    public @Nullable BlockState state() {
        return state;
    }

    public int y() {
        return pos != null ? pos.getY() : 0;
    }

    //0 when there's none. real ones are opaque
    public int sampleOriginal() {
        if (original == null || state == null || computingOriginal) return 0;
        if (!hasOriginalColor) {
            computingOriginal = true;
            try {
                originalColor = original.getColor(state, level, pos, tintIndex) | 0xFF000000;
            } finally {
                computingOriginal = false;
            }
            hasOriginalColor = true;
        }
        return originalColor;
    }
}
