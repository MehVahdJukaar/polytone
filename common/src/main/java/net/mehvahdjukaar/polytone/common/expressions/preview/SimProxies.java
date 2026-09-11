package net.mehvahdjukaar.polytone.common.expressions.preview;

import java.util.ArrayList;
import java.util.List;

// Installed via PreviewContext
public final class SimProxies {

    public final SimGlobalProxy global = new SimGlobalProxy();
    public final SimCameraProxy camera = new SimCameraProxy();
    public final SimPlayerProxy player = new SimPlayerProxy();

    private final List<SimValue> values;

    public SimProxies() {
        List<SimValue> all = new ArrayList<>(global.values());
        all.addAll(camera.values());
        all.addAll(player.values());
        this.values = List.copyOf(all);
    }

    public List<SimValue> values() {
        return values;
    }

    public void clearReads() {
        for (SimValue v : values) v.clearRead();
    }
}
