package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGetters;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@BeanGetters
public class CameraProxy {

    private Camera instance(){
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public Vec3 pos(){
        return instance().position();
    }

    public double x(){
        return instance().position().x;
    }

    public double y() {
        return instance().position().y;
    }

    public float yaw(){
        return instance().yaw();
    }

    public float pitch(){
        return Mth.wrapDegrees(instance().xRot());
    }

}
