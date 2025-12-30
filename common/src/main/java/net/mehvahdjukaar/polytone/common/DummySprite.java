package net.mehvahdjukaar.polytone.common;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

public class DummySprite extends TextureAtlasSprite {
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath("polytone", "unit");
    public static final DummySprite INSTANCE = new DummySprite();

    private DummySprite() {
        super(LOCATION, new SpriteContents(LOCATION, new FrameSize(1, 1), new NativeImage(1, 1, false)), 1, 1, 0, 0, 0);
    }

    public float getU(float u) {
        return u;
    }

    public float getV(float v) {
        return v;
    }
}