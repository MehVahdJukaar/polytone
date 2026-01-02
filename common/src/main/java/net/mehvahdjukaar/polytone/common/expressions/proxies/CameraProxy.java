package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class CameraProxy {

    private Camera instance(){
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public Vec3 x(){
        return instance().position();
    }
}
