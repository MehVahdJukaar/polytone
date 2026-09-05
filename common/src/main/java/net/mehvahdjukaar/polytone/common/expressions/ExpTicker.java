package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

//move to global?
public class ExpTicker {

    private static double dayTime;
    private static float rainAndThunder;
    private static float season;

    private static DimensionType lastDimType;
    private static Screen lastScreen = null;
    private static int screenTime;

    public static void onTick(Level level) {
        screenTime++;
        Minecraft mc = Minecraft.getInstance();
        refreshPlayerSnapshot(); // before the level check: clears to null when leaving a world
        if (level == null) return;
        if (level.dimensionType() != lastDimType) {
            lastDimType = level.dimensionType();
            Polytone.onDimChanged(level);
        }

        //TODO: oter param like moon pos
        rainAndThunder = level.getRainLevel(0) * 0.5f + level.getThunderLevel(0) * 0.5f;
        season = ISeason.getNumber(level);

        if (mc.gui.screen() != lastScreen) {
            lastScreen = mc.gui.screen();
            screenTime = 0;
        }
    }

    //onTick runs at tick start, before entities move, so the async particle batch re-refreshes this
    //right before dispatching or fast players get trailed by a whole tick. volatile publishes it to the workers
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
        return rainAndThunder;
    }

    public static float getSeasonNumber() {
        return season;
    }
}
