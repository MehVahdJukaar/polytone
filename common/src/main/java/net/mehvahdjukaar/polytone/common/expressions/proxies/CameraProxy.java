package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import static net.mehvahdjukaar.polytone.common.expressions.ExpUtils.parseEnvAttr;

@BeanGettersAliases
public class CameraProxy extends PositionalProxy {

    public static final CameraProxy INSTANCE = new CameraProxy();

    public CameraProxy() {
        super();
    }

    private Camera delegate(){
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    @Override
    protected BlockPos getPosInternal() {
        return  delegate().blockPosition();
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

    public float yaw() {
        return delegate().yaw();
    }

    public float pitch() {
        return Mth.wrapDegrees(delegate().xRot());
    }

    public float roll() {
        return Mth.wrapDegrees(PlatStuff.getCamRoll(delegate()));
    }

    public boolean detatched() {
        return delegate().isDetached();
    }

    @Override
    public Object environmentAttribute(String attributeName) {
        var env = parseEnvAttr(attributeName);
        return delegate().attributeProbe().getValue(env, 0);

    }
}
