package net.mehvahdjukaar.polytone.misc;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;

public abstract class GenericDirectorySpriteSource implements SpriteSource {

    /*
    public static final MapCodec<GenericDirectorySpriteSource> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
            Codec.STRING.fieldOf("source").forGetter((d) -> d.sourcePath),
            Codec.STRING.fieldOf("prefix").forGetter((d) -> d.idPrefix)
    ).apply(i, GenericDirectorySpriteSource::new));

    public static final SpriteSourceType TYPE =
            SpriteSources.register("polytone_generic_directory", GenericDirectorySpriteSource.CODEC);

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
    }*/
}
