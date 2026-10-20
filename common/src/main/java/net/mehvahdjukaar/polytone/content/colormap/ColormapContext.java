package net.mehvahdjukaar.polytone.content.colormap;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

//mega hack to pass more xontext down colormap call chain
public class ColormapContext {

    private @Nullable BlockAndTintGetter level;
    private @Nullable BlockState state;
    private @Nullable BlockPos pos;
    private @Nullable BlockTintSource owner;
    private boolean computingOriginal;
    private boolean hasOriginalColor;
    private int originalColor;

    public void startBlock(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable BlockPos pos,
                           @Nullable BlockTintSource owner) {
        this.level = level;
        this.state = state;
        this.pos = pos;
        this.owner = owner;
        this.hasOriginalColor = false;
    }

    public void endBlock() {
        this.state = null;
        this.pos = null;
        this.owner = null;
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
        if (owner == null || state == null || computingOriginal) return 0;
        if (!hasOriginalColor) {
            computingOriginal = true;
            try {
                BlockTintSource original = Polytone.BLOCK_MODIFIERS.getVanillaColor(state, owner);
                if (original == null) originalColor = 0;
                else if (level == null || pos == null) originalColor = original.color(state) | 0xFF000000;
                else originalColor = original.colorInWorld(state, level, pos) | 0xFF000000;
            } finally {
                computingOriginal = false;
            }
            hasOriginalColor = true;
        }
        return originalColor;
    }
}
