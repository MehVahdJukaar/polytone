package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public record ParticleColor(IColorGetter getter, CachePolicy policy) {

    private static final SchemaCodec<ParticleColor> WITH_OPTIONS = SchemaRecord.create(ParticleColor.class, i -> i.group(
            i.field("colormap", Colormap.CODEC, ParticleColor::getter),
            i.optional("cache", CachePolicy.CODEC, CachePolicy.NONE, ParticleColor::policy)
    ).apply(i, ParticleColor::new));

    public static final SchemaCodec<ParticleColor> CODEC = SchemaCodecs.withAlternative(
            SchemaCodecs.alt("with options", WITH_OPTIONS),
            SchemaCodecs.alt("colormap", SchemaCodecs.xmap(Colormap.CODEC,
                    g -> new ParticleColor(g, CachePolicy.NONE),
                    ParticleColor::getter)));


    public enum CachePolicy implements StringRepresentable {
        ON_SPAWN,
        PER_POSITION,
        NONE;

        public static final Codec<CachePolicy> CODEC = StringRepresentable.fromEnum(CachePolicy::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}