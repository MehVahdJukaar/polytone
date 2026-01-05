package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@BeanGettersAliases
public class CameraProxy {
    public static final CameraProxy INSTANCE = new CameraProxy();

    private Camera camera;

    private Camera getCameraInternal() {
        if (camera == null) {
            camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        }
        return camera;
    }

    public double x() {
        return getCameraInternal().position().x;
    }

    public double y() {
        return getCameraInternal().position().y;
    }

    public  double z() {
        return getCameraInternal().position().z;
    }

    public float yaw() {
        return getCameraInternal().yaw();
    }

    public float pitch() {
        return Mth.wrapDegrees(getCameraInternal().xRot());
    }

    public float roll() {
        return Mth.wrapDegrees(getCameraInternal().zrot());
    }

}
