package net.mehvahdjukaar.polytone.content.packinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.metadata.MetadataSectionType;

import java.util.Optional;

public record PackInfo(Optional<Component> title, Optional<Component> content) {

    public static final Codec<PackInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PackInfo::title),
            ComponentSerialization.CODEC.optionalFieldOf("content").forGetter(PackInfo::content)
    ).apply(i, PackInfo::new));

    public static final MetadataSectionType<PackInfo> TYPE = MetadataSectionType.fromCodec("polytone", CODEC);

    public boolean isEmpty() {
        return title.isEmpty() && content.isEmpty();
    }
}
