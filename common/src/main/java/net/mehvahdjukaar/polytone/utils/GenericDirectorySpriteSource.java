package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class GenericDirectorySpriteSource implements SpriteSource {



    public static final MapCodec<GenericDirectorySpriteSource> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
            Codec.STRING.fieldOf("source").forGetter((d) -> d.sourcePath),
            Codec.STRING.fieldOf("prefix").forGetter((d) -> d.idPrefix)
    ).apply(i, GenericDirectorySpriteSource::new));

    public static final SpriteSourceType TYPE = SpriteSources.register("polytone_generic_directory", GenericDirectorySpriteSource.CODEC);

    public static void init() {
    }

    private final String sourcePath;
    private final String idPrefix;

    public GenericDirectorySpriteSource(String sourcePath, String idPrefix) {
        this.sourcePath = sourcePath;
        this.idPrefix = idPrefix;
    }

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        FileToIdConverter fileToIdConverter = new FileToIdConverter(this.sourcePath, ".png");
        fileToIdConverter.listMatchingResources(resourceManager).forEach((resourceLocation, resource) -> {
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation).withPrefix(this.idPrefix);
            output.add(resourceLocation2, resource);
        });
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
