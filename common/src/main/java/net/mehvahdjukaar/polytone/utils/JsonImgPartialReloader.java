package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public abstract class JsonImgPartialReloader<O> extends ContentManager<O, JsonImgPartialReloader.Resources> {

    /** Non-editable variant (no file codec): the manager won't appear in the pack editor. */
    protected JsonImgPartialReloader(String name, String... folders) {
        super(name, folders);
    }

    protected JsonImgPartialReloader(String name, @Nullable Supplier<? extends SchemaCodec<O>> codec, String... folders) {
        super(name, codec, folders);
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        var textures = this.getImagesInDirectories(resourceManager);

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures) {
    }


}
