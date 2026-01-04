package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum LiquidAffinity implements StringRepresentable {
    LIQUIDS, NON_LIQUIDS, ANY;

    public static final Codec<LiquidAffinity> CODEC = StringRepresentable.fromEnum(LiquidAffinity::values);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}

