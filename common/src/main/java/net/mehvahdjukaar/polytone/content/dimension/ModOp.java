package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ModOp implements StringRepresentable {
    REPLACE,
    REMOVE;

    public static final Codec<ModOp> CODEC = StringRepresentable.fromEnum(ModOp::values);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
