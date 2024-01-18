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
            profiler.push(c.path() + "_prepare");
            list.add(c.prepare(resourceManager));
            profiler.pop();
        }
        return list;
    }

    @Override
    protected void apply(List<Object> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (var c : children) {
            profiler.push(c.path() + "_reset");
            c.reset();
        }

        for (int i = 0; i < object.size(); i++) {
            PartialReloader<?> c = children.get(i);
            profiler.push(c.path() + "_process");
            processTyped(c, object.get(i));
            profiler.pop();
        }

        for (var c : children) {
            profiler.push(c.path() + "_apply");
            c.apply();
            profiler.pop();
        }
    }

    @SuppressWarnings("all")
    private <T> void processTyped(PartialReloader<T> reloader, Object object) {
        reloader.process((T) object);
    }
}
