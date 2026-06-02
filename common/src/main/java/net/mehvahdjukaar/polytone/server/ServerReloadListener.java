package net.mehvahdjukaar.polytone.server;

import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public abstract class ServerReloadListener extends PartialReloader implements PreparableReloadListener {

    /*
    public ServerReloadListener() {
        super(new Gson(), "polytone/block_modifiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();
            // do something with the json
            ServerBlockModifier prop = ServerBlockModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Server Block Property with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
        }
    }

    @Override
    protected Object prepare(ResourceManager resourceManager) {
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
