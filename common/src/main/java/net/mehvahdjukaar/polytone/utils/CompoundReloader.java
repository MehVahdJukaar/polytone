package net.mehvahdjukaar.polytone.utils;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;

// Needed to reload stuff in order but still off-thread when we can in prepare
public class CompoundReloader extends SimplePreparableReloadListener<List<Object>> {

    private final List<PartialReloader<?>> children;

    public CompoundReloader(PartialReloader<?>... reloaders) {
        children = List.of(reloaders);
    }

    @Override
    protected List<Object> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        //sequentially prepares all of them in order
        List<Object> list = new ArrayList<>();
        for (var c : children) {
            list.add(c.prepare(resourceManager));
        }
        return list;
    }

    @Override
    protected void apply(List<Object> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (var c : children) {
            c.reset();
        }

        for (int i = 0; i < object.size(); i++) {
            PartialReloader<?> c = children.get(i);
            processTyped(c, object.get(i));
        }

        for (var c : children) {
            c.apply();
        }
    }

    @SuppressWarnings("all")
    private <T> void processTyped(PartialReloader<T> reloader, Object object) {
        reloader.process((T) object);
    }
}
