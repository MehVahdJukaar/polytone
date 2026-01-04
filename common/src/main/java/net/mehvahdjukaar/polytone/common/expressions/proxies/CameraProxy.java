package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@BeanGettersAliases
public class CameraProxy {
    public static final CameraProxy INSTANCE = new CameraProxy();

    private Camera delegate(){
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public Vec3 pos(){
        return delegate().position();
    }

    public double x(){
        return delegate().position().x;
    }

    public double y() {
        return delegate().position().y;
    }

    public float yaw(){
        return delegate().yaw();
    }

    public float pitch(){
        return Mth.wrapDegrees(delegate().xRot());
    }

}
