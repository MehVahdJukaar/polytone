package net.mehvahdjukaar.polytone.content.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@BeanAliases
public class CameraProxy extends PositionalProxy {

    public static final CameraProxy INSTANCE = new CameraProxy();

    public CameraProxy() {
        super();
    }

    private Camera delegate() {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    @Override
    protected BlockPos getPosInternal() {
        return delegate().getBlockPosition();
    }

    @Override
    protected Level getLevelInternal() {
        return Minecraft.getInstance().level;
    }

    public double x() {
        return delegate().getPosition().x;
    }

    public double y() {
        return delegate().getPosition().y;
    }

    public double z() {
        return delegate().getPosition().z;
    }

    public double yaw() {
        return delegate().getYRot();
    }

    public double pitch() {
        return Mth.wrapDegrees(delegate().getXRot());
    }

    public double roll() {
        // Stub - PlatStuff.getCamRoll not present on 1.21.1
        return 0;
    }

    public boolean detatched() {
        return delegate().isDetached();
    }

    public double viewDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16.0;
    }

    long lastFovUpdate = -1;
    double cachedFov = -1;

    public double fov() {
        // 1.21.1's GameRenderer.getFov is private. Fall back to the user's FOV option.
        return Minecraft.getInstance().options.fov().get();
    }

    public boolean lookingToward(double x, double y, double z) {
        // 1.21.1 Camera lacks forwardVector(); reconstruct it from rotation
        Camera camera = delegate();
        Vector3f f = new Vector3f(0, 0, 1).rotate(camera.rotation());
        var dirVec = new Vec3(f);
        var camPos = camera.getPosition();
        var toTarget = new Vec3(x - camPos.x, y - camPos.y, z - camPos.z).normalize();
        double dot = dirVec.dot(toTarget);
        if (dot < 0) return false;
        double fovAngleDeg = this.fov();
        double threshold = Math.cos(Math.toRadians(fovAngleDeg / 2.0));
        return dot >= threshold;
    }

    // Stub - EnvironmentAttribute system doesn't exist on 1.21.1
    @Override
    public Object environmentAttribute(String attributeName) {
        return 0;
    }
}
