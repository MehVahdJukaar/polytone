package net.mehvahdjukaar.polytone.utils;

import com.google.gson.Gson;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.resources.ResourceManager;

public abstract class PartialReloader<T> {

    public static final Gson GSON = new Gson();

    protected String name;

    protected PartialReloader(String name) {
        this.name = name;
    }

    public String path() {
        return Polytone.MOD_ID + "/" + name;
    }

    protected abstract T prepare(ResourceManager resourceManager);

    protected abstract void reset();

    protected abstract void process(T obj);

    protected void apply(){};
}
