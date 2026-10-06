package net.mehvahdjukaar.polytone.content.packinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.metadata.MetadataSectionType;

import java.util.Optional;

public record PackInfo(Optional<Component> title, Optional<Component> content, Optional<OpenGLVersion> minOpenGL) {

    public static final Codec<PackInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PackInfo::title),
            ComponentSerialization.CODEC.optionalFieldOf("content").forGetter(PackInfo::content),
            OpenGLVersion.CODEC.optionalFieldOf("min_opengl").forGetter(PackInfo::minOpenGL)
    ).apply(i, PackInfo::new));

    public static final MetadataSectionType<PackInfo> TYPE = MetadataSectionType.fromCodec("polytone", CODEC);

    public boolean hasInfoPage() {
        return title.isPresent() || content.isPresent();
    }

    public boolean isEmpty() {
        return !hasInfoPage() && minOpenGL.isEmpty();
    }

    public record OpenGLVersion(int major, int minor) {

        public static final Codec<OpenGLVersion> CODEC = Codec.STRING.comapFlatMap(OpenGLVersion::parse, OpenGLVersion::toString);

        private static DataResult<OpenGLVersion> parse(String s) {
            String[] parts = s.split("\\.");
            try {
                if (parts.length == 2) return DataResult.success(new OpenGLVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
            } catch (NumberFormatException ignored) {
            }
            return DataResult.error(() -> "OpenGL version must look like \"4.5\", got \"" + s + "\"");
        }

        @Override
        public String toString() {
            return major + "." + minor;
        }
    }
}
