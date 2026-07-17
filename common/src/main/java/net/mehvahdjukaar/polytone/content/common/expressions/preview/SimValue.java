package net.mehvahdjukaar.polytone.content.common.expressions.preview;

public final class SimValue {

    private final String label;
    private final double min, max, step;
    private double value;
    private boolean read;

    private SimValue(String label, double min, double max, double def, double step) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = def;
    }

    public static SimValue slider(String label, double min, double max, double def, double step) {
        return new SimValue(label, min, max, def, step);
    }

    /** Called by the sim proxy accessors: marks the input as read and returns the slider value. */
    public double get() {
        read = true;
        return value;
    }

    public String label() {
        return label;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public double value() {
        return value;
    }

    public void set(double value) {
        this.value = value;
    }

    public boolean wasRead() {
        return read;
    }

    public void clearRead() {
        read = false;
    }
}
