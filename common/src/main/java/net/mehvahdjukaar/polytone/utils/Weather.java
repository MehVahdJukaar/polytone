package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.StringRepresentable;

public enum Weather implements StringRepresentable {
    CLEAR, RAIN, THUNDER;


    public static Weather get(ClientLevel level) {
        if (level != null && level.isRaining()) {
            return level.isThundering() ? THUNDER : RAIN;
        }
        return CLEAR;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }


    public static final Codec<Weather> CODEC = StringRepresentable.fromEnum(Weather::values);
}