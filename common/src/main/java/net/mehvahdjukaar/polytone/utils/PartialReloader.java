package net.mehvahdjukaar.polytone.utils;

import net.minecraft.server.packs.resources.ResourceManager;

public abstract class PartialReloader<T> {

    protected String name;

    PartialReloader(String name) {
        this.name = name;
    }

    abstract T prepare(ResourceManager resourceManager);

    abstract void reset();

    abstract void process(T obj);

    abstract void apply();
}
