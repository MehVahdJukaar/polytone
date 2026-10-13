package net.mehvahdjukaar.polytone.content.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

//static state that tracks level passes for reentrancy
public final class LevelRenderPassTracker {

    private static boolean inVanillaFrame = false;
    private static final ArrayDeque<Pass> passesStack = new ArrayDeque<>();

    public static void onStartRenderLevel() {
        inVanillaFrame = true;
        passesStack.clear();
    }

    public static void onEndRenderLevel() {
        inVanillaFrame = false;
    }

    public static boolean pushAndIsMain() {
        passesStack.push(new Pass(Minecraft.getInstance().level));
        return inVanillaFrame && passesStack.size() == 1;
    }

    public static boolean popAndWasMain() {
        passesStack.poll();
        return inVanillaFrame && passesStack.isEmpty();
    }

    //if we are even rendering a level or outside like in GUI
    public static boolean isInLevelPass() {
        return !passesStack.isEmpty();
    }

    @Nullable
    public static ClientLevel currentLevel() {
        Pass pass = passesStack.peek();
        return pass != null ? pass.level : Minecraft.getInstance().level;
    }

    private record Pass(@Nullable ClientLevel level) {
    }
}
