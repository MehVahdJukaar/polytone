package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public abstract class JsonPartialReloader<O> extends ContentManager<O, Map<ResourceLocation, JsonElement>> {

    /** Non-editable variant (no file codec): the manager won't appear in the pack editor. */
    protected JsonPartialReloader(String name, String... folders) {
        super(name, folders);
    }

    protected JsonPartialReloader(String name, @Nullable Supplier<? extends SchemaCodec<O>> codec, String... folders) {
        super(name, codec, folders);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        return ImmutableMap.copyOf(jsons);
    }
}
