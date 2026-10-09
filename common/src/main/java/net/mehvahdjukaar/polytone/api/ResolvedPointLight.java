package net.mehvahdjukaar.polytone.api;

public record ResolvedPointLight(int color, float radius, float brightness) {

    public ResolvedPointLight(int color, float radius) {
        this(color, radius, 1);
    }

}
