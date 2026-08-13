package net.mehvahdjukaar.polytone.content.common.expressions;

import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

// On 1.21.1 the per-tick climate/season state is owned by ClientFrameTicker.
// This class is a thin facade so MVEL expressions can read it under the same API as 1.21.11.
public class ExpTicker {

    private static Screen lastScreen = null;
    private static int screenTime;

    public static void onTick() {
        screenTime++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != lastScreen) {
            lastScreen = mc.screen;
            screenTime = 0;
        }
        refreshPlayerSnapshot(); // keep the async player-stats cache in step with the tick
    }

    // Re-refreshed by PolytoneAsyncParticles right before it dispatches its parallel batch (a stale
    // position would make player-following particles trail a fast player by a whole tick). Workers
    // only ever read this cache, never the live entity; the volatile reference publishes it.
    private static volatile @Nullable PlayerSnapshot playerSnapshot = null;

    public record PlayerSnapshot(double x, double y, double z,
                                 double xd, double yd, double zd,
                                 double speed, double width, double height,
                                 boolean crouching) {
    }

    // Re-caches the player's stats (main thread). No player -> null, proxies fall back to live reads.
    public static void refreshPlayerSnapshot() {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            playerSnapshot = null;
            return;
        }
        var dm = p.getDeltaMovement();
        playerSnapshot = new PlayerSnapshot(p.getX(), p.getY(), p.getZ(),
                dm.x, dm.y, dm.z,
                dm.length(), p.getBbWidth(), p.getBbHeight(),
                p.isCrouching());
    }

    public static @Nullable PlayerSnapshot playerSnapshot() {
        return playerSnapshot;
    }

    public static int getGuiTime() {
        return screenTime;
    }

    public static float getRainAndThunder() {
        return ClientFrameTicker.getRainAndThunder();
    }

    public static float getSeasonNumber() {
        return ClientFrameTicker.getSeason();
    }
}
