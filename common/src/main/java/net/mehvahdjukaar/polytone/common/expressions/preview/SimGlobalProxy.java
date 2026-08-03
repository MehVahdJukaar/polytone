package net.mehvahdjukaar.polytone.common.expressions.preview;

import net.mehvahdjukaar.polytone.compat.ISeason;
import net.mehvahdjukaar.polytone.common.expressions.proxies.GlobalProxy;

import java.util.List;

public final class SimGlobalProxy extends GlobalProxy {

    // These MUST NOT be public: MVEL's PropertyTools.getFieldOrAccessor resolves a public field
    // before the bean getter, so a public field named like an accessor (rain -> rain()) would make
    // g.rain return this SimValue object instead of invoking the overridden accessor - the read
    // would never be recorded and the slider would never show. Private keeps them out of
    // Class.getFields(), forcing MVEL onto the generated getRain() (which virtual-dispatches here).
    private final SimValue gameTime = SimValue.slider("Game time", 0, 24000, 0, 100);
    private final SimValue dayTime = SimValue.slider("Day time", 0, 24000, 6000, 100);
    private final SimValue rain = SimValue.slider("Rain / thunder", 0, 1, 0, 0.05);
    private final SimValue season = SimValue.slider("Season", 0, 1, 0, 0.02);

    private final List<SimValue> values = List.of(gameTime, dayTime, rain, season);

    /** All inputs, in UI display order. */
    public List<SimValue> values() {
        return values;
    }

    public void clearReads() {
        for (SimValue v : values) v.clearRead();
    }

    @Override
    public double time() {
        return gameTime.get();
    }

    @Override
    public double dayTime() {
        return dayTime.get();
    }

    @Override
    public double rain() {
        return rain.get();
    }

    @Override
    public double seasonNumber() {
        return season.get();
    }

    // Sub-seasons are ordered spring->winter (3 per season), so quartiles of the 0..1 season number
    // map back onto the calendar season for g.season() string comparisons. Routing through
    // seasonNumber() also records the read, so the one season slider serves both accessors.
    @Override
    public String season() {
        ISeason[] order = {ISeason.SPRING, ISeason.SUMMER, ISeason.AUTUMN, ISeason.WINTER};
        int idx = (int) Math.floor(Math.max(0, Math.min(0.999, seasonNumber())) * 4);
        return order[idx].lowercaseName();
    }
}
