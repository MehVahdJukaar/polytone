package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@BeanAliases
public class PlayerProxy extends AbstractEntityProxy {

    public static final PlayerProxy INSTANCE = new PlayerProxy();

    public PlayerProxy() {
    }

    // Per-tick snapshot of hot player values, filled on the main thread before the parallel batch
    // so worker expression reads become cached fields. Consulted only while snapshotActive.
    private static volatile boolean snapshotActive = false;
    private static double sX, sY, sZ, sXd, sYd, sZd, sSpeed, sWidth, sHeight;
    private static boolean sCrouching;

    /** Snapshot the current player (call on the main thread). No-op if there is no player. */
    public static void beginSnapshot() {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            snapshotActive = false;
            return;
        }
        sX = p.getX();
        sY = p.getY();
        sZ = p.getZ();
        var dm = p.getDeltaMovement();
        sXd = dm.x;
        sYd = dm.y;
        sZd = dm.z;
        sSpeed = dm.length();
        sWidth = p.getBbWidth();
        sHeight = p.getBbHeight();
        sCrouching = p.isCrouching();
        snapshotActive = true; // volatile write publishes the fields written above
    }

    public static void endSnapshot() {
        snapshotActive = false;
    }

    @Override
    protected Player entity() {
        return Minecraft.getInstance().player;
    }

    @Override
    protected LivingEntity livingEntity() {
        return Minecraft.getInstance().player;
    }

    @Override
    public double x() {
        return snapshotActive ? sX : super.x();
    }

    @Override
    public double y() {
        return snapshotActive ? sY : super.y();
    }

    @Override
    public double z() {
        return snapshotActive ? sZ : super.z();
    }

    @Override
    public double xd() {
        return snapshotActive ? sXd : super.xd();
    }

    @Override
    public double yd() {
        return snapshotActive ? sYd : super.yd();
    }

    @Override
    public double zd() {
        return snapshotActive ? sZd : super.zd();
    }

    @Override
    public double speed() {
        return snapshotActive ? sSpeed : super.speed();
    }

    @Override
    public double width() {
        return snapshotActive ? sWidth : super.width();
    }

    @Override
    public double height() {
        return snapshotActive ? sHeight : super.height();
    }

    @Override
    public boolean crouching() {
        return snapshotActive ? sCrouching : super.crouching();
    }

    public double itemUseTicks(){
        return entity().getTicksUsingItem();
    }

    public String itemUsed(){
        return entity().getUseItem().getItemHolder().getRegisteredName();
    }
}
