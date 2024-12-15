package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Stopwatch;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
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
        //sequentially prepares all of them in order. Whole point of this is that we cant multi thread this part. This still happens off-thread
        List<Object> list = new ArrayList<>();
        for (var c : children) {
            list.add(c.prepare(resourceManager));
        }
        return list;
    }

    @Override
    protected void apply(List<Object> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        // clear existing lazy holder sets
        LazyHolderSet.clearAll();

        Stopwatch stopwatch = Stopwatch.createStarted();
        for (var c : children) {
            c.resetWithLevel(false);
            c.reset();
        }


        for (int i = 0; i < object.size(); i++) {
            PartialReloader<?> c = children.get(i);
            try {
                processTyped(c, object.get(i));
            } catch (Exception e) {
                String message = c + " failed to parse some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }

        if (Minecraft.getInstance().level != null) {
            try {
                LazyHolderSet.initializeAll(Minecraft.getInstance().level.registryAccess());
            } catch (Exception e) {
                String message = "failed to parse some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }

        for (var c : children) {
            try {
                c.apply();
                if (level != null) c.applyWithLevel(level.registryAccess(), false);
            } catch (Exception e) {
                String message = c + " failed to apply some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }

        Polytone.LOGGER.info("Reloaded Polytone Resources in {} ms", stopwatch.elapsed().toMillis());
    }

    @SuppressWarnings("all")
    private <T> void processTyped(PartialReloader<T> reloader, Object object) {
        //yea... we cant use registry ops here theres no level yet
        reloader.process((T) object, JsonOps.INSTANCE);
    }

    public void applyOnLevelLoad(RegistryAccess registryAccess, boolean firstLogin) {
        for (var c : children) {
            c.applyWithLevel(registryAccess, firstLogin);
        }
    }

    public void resetOnLevelUnload() {
        for (var c : children) {
            c.resetWithLevel(true);
        }
    }
}
