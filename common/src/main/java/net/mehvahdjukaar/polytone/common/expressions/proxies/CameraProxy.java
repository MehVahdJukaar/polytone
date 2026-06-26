package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Locale;

import static net.mehvahdjukaar.polytone.common.expressions.ExpUtils.parseEnvAttr;

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
        return delegate().blockPosition();
    }

    @Override
    protected Level getLevelInternal() {
        return Minecraft.getInstance().level;
    }

    public double x() {
        return delegate().position().x;
    }

    public double y() {
        return delegate().position().y;
    }

    public double z() {
        return delegate().position().z;
    }

    public double yaw() {
        return delegate().yaw();
    }

    public double pitch() {
        return Mth.wrapDegrees(delegate().xRot());
    }

    public double roll() {
        return Mth.wrapDegrees(PlatStuff.getCamRoll(delegate()));
    }

    public boolean detatched() {
        return delegate().isDetached();
    }

    public boolean firstPerson(){
     return Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
    }

    public String type(){
        return Minecraft.getInstance().options.getCameraType().toString().toLowerCase(Locale.ROOT);
    }

    public double viewDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16.0;
    }

    long lastFovUpdate = -1;
    double cachedFov = -1;

    public double fov() {
        Camera camera = delegate();
        long nowTime = Minecraft.getInstance().level.getGameTime();
        if (nowTime != lastFovUpdate) {
            lastFovUpdate = nowTime;
            cachedFov = Minecraft.getInstance().gameRenderer.getFov(camera, 0, true);
        }
        return cachedFov;
    }

    public boolean lookingToward(double x, double y, double z) {
        var dirVec = new Vec3(delegate().forwardVector().normalize(new Vector3f()));
        var camPos = delegate().position();
        var toTarget = new Vec3(x - camPos.x, y - camPos.y, z - camPos.z).normalize();
        double dot = dirVec.dot(toTarget);
        if (dot < 0) return false;
        double fovAngleDeg = this.fov();
        double threshold = Math.cos(Math.toRadians(fovAngleDeg / 2.0));
        return dot >= threshold;
    }

    @Override
    public Object environmentAttribute(String attributeName) {
        var env = parseEnvAttr(attributeName);
        return delegate().attributeProbe().getValue(env, 0);

    }
}
