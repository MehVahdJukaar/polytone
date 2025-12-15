package net.mehvahdjukaar.polytone.misc;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;

public class DummySprite extends TextureAtlasSprite {
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("polytone", "unit");
    public static final DummySprite INSTANCE = new DummySprite();

    private DummySprite() {
        super(LOCATION, new SpriteContents(LOCATION, new FrameSize(1, 1), new NativeImage(1, 1, false)), 1, 1, 0, 0);
    }

    public float getU(float u) {
        return u;
    }

    public float getV(float v) {
        return v;
    }
}