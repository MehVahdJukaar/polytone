package net.mehvahdjukaar.polytone.lightmap;

import net.mehvahdjukaar.polytone.utils.ArrayImage;

public class Lightmap {
    private final ArrayImage image;
    private final boolean hasNightVision;

    public Lightmap(ArrayImage image) {
        this.image = image;
        hasNightVision = image.height() > 32;
    }
}
