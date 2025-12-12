package net.mehvahdjukaar.polytone.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class ServerReloadListener extends PartialReloader implements PreparableReloadListener {

    /*
    public ServerReloadListener() {
        super(new Gson(), "polytone/block_modifiers");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (Map.Entry<Identifier, JsonElement> entry : object.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement json = entry.getValue();
            // do something with the json
            ServerBlockModifier prop = ServerBlockModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Server Block Property with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
        }
    }

    @Override
    protected Object prepare(PreparableReloadListener.SharedState sharedState) {
        return null;
    }

    @Override
    protected void reset() {

    }

    @Override
    protected void process(Object obj, DynamicOps ops) {

    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return null;
    }

     */
}
