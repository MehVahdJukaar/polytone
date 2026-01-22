package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TickSource implements StringRepresentable {
    ANIMATE_TICK, BLOCK_BROKEN, BLOCK_CRACKING;
    //, SCHEDULED_TICK, RANDOM_TICK too bad these 2 are server side logic...
    ;

    public static final Codec<TickSource> CODEC = StringRepresentable.fromEnum(TickSource::values);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}