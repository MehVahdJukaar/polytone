package net.mehvahdjukaar.polytone.content.particle.custom;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public class SpritePicker {
    private final boolean selectsRandom;
    @Nullable
    private SpriteSet set;

    public SpritePicker(boolean selectsRandom) {
        this.selectsRandom = selectsRandom;
    }

    public void acceptSprites(SpriteSet spriteSet) throws SpriteSetErrorException {
        if (this.set != null) {
            throw new SpriteSetErrorException("Sprites already set!");
        }
        //assert non empty
        try {
            spriteSet.get(0, 1);
        } catch (Exception e) {
            //crash so pack devs know
            throw new SpriteSetErrorException("SpriteSet content was empty!");
        }
        this.set = spriteSet;
    }

    public boolean hasSprites() {
        return set != null;
    }

    public @Nullable SpriteSet getSpriteSet() {
        return set;
    }

    public TextureAtlasSprite getAny() {
        if (set == null) {
            throw new RuntimeException("Tried to create a particle with no sprites set!");
        }
        return set.get(0, 1);
        /*
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(
                TextureAtlas.LOCATION_PARTICLES
        ).missingSprite();
        */
    }

    public void pickSprite(CustomParticleInstance particle, boolean init) {
        if (set == null) {
            throw new RuntimeException("Tried to pick a sprite for a particle with no sprites set!");
        }
        if (this.selectsRandom) {
            if (init) particle.setSprite(set.get(particle.getRandom()));
        } else {
            particle.setSpriteFromAge(set);
        }

    }

    public boolean selectsRandom() {
        return selectsRandom;
    }
}