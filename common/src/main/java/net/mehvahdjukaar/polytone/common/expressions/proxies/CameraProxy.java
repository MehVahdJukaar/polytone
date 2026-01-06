package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

@BeanGettersAliases
public class CameraProxy extends PositionalProxy {

    public static final CameraProxy INSTANCE = create();

    protected final Camera camera;

    private CameraProxy(Camera camera, Level level, BlockPos pos) {
        super(level, pos);
        this.camera = camera;
    }

    public static CameraProxy create() {
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        BlockPos posVec = cam.blockPosition();
        return new CameraProxy(cam, mc.level, posVec);
    }

    public double x() {
        return camera.position().x;
    }

    public double y() {
        return camera.position().y;
    }

    public double z() {
        return camera.position().z;
    }

    public float yaw() {
        return camera.yaw();
    }

    public float pitch() {
        return Mth.wrapDegrees(camera.xRot());
    }

    public float roll() {
        return Mth.wrapDegrees(PlatStuff.getCamRoll(camera));
    }

}
