package net.mehvahdjukaar.polytone.common.expressions.preview;

import net.mehvahdjukaar.polytone.common.expressions.proxies.PlayerProxy;

import java.util.List;

// Position and speed come from editor sliders instead of the live player (also avoiding NPEs with no
// world loaded). Must stay a subclass for the MVEL accessor-cache reason noted in SimGlobalProxy.
public final class SimPlayerProxy extends PlayerProxy {

    // Private, not public: a public field named like an accessor (x -> x()) wins over the bean
    // getter in MVEL, so g/p.x would read this SimValue instead of the overridden accessor. See
    // SimGlobalProxy for the full explanation.
    private final SimValue x = SimValue.slider("Player X", -256, 256, 0, 1);
    private final SimValue y = SimValue.slider("Player Y", -64, 320, 64, 1);
    private final SimValue z = SimValue.slider("Player Z", -256, 256, 0, 1);
    private final SimValue speed = SimValue.slider("Player speed", 0, 1, 0, 0.01);

    private final List<SimValue> values = List.of(x, y, z, speed);

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
    public double speed() {
        return speed.get();
    }
}
