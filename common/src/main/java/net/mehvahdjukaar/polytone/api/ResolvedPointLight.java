package net.mehvahdjukaar.polytone.api;

//radius in blocks, brightness multiplies the color
public record ResolvedPointLight(int color, float radius, float brightness) {

    public ResolvedPointLight(int color, float radius) {
        this(color, radius, 1);
    }
}
