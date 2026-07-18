package net.mehvahdjukaar.polytone.content.common.expressions.preview;

import net.mehvahdjukaar.polytone.content.common.expressions.proxies.CameraProxy;

import java.util.List;

/**
 * Preview-only stand-in for {@link CameraProxy}: position and rotation come from editor sliders
 * instead of the live camera. Must stay a subclass for the same MVEL accessor-cache reason as
 * {@link SimGlobalProxy}. Level-backed queries inherited from PositionalProxy stay live.
 */
public final class SimCameraProxy extends CameraProxy {

    // Private, not public: a public field named like an accessor (x -> x()) wins over the bean
    // getter in MVEL, so g/c.x would read this SimValue instead of the overridden accessor. See
    // SimGlobalProxy for the full explanation.
    private final SimValue x = SimValue.slider("Camera X", -256, 256, 0, 1);
    private final SimValue y = SimValue.slider("Camera Y", -64, 320, 64, 1);
    private final SimValue z = SimValue.slider("Camera Z", -256, 256, 0, 1);
    private final SimValue yaw = SimValue.slider("Camera yaw", -180, 180, 0, 1);
    private final SimValue pitch = SimValue.slider("Camera pitch", -90, 90, 0, 1);

    private final List<SimValue> values = List.of(x, y, z, yaw, pitch);

    public List<SimValue> values() {
        return values;
    }

    @Override
    public double x() {
        return x.get();
    }

    @Override
    public double y() {
        return y.get();
    }

    @Override
    public double z() {
        return z.get();
    }

    @Override
    public double yaw() {
        return yaw.get();
    }

    @Override
    public double pitch() {
        return pitch.get();
    }
}
