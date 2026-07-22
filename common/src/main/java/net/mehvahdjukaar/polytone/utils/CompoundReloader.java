package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

// Needed to reload stuff in order but still off-thread when we can in prepare
public class CompoundReloader implements PreparableReloadListener {

    private final List<PartialReloader<?>> children;
    private final List<?> childrenResourcesCache = new ArrayList<>();

    public CompoundReloader(PartialReloader<?>... reloaders) {
        children = List.of(reloaders);
    }


    @Override
    public final CompletableFuture<Void> reload(
            PreparableReloadListener.PreparationBarrier preparationBarrier,
            ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor
    ) {
        List<CompletableFuture<?>> futures = children.stream()
                .map(child -> CompletableFuture.supplyAsync(() -> child.prepare(resourceManager), backgroundExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(preparedList -> {
                    childrenResourcesCache.clear();
                    childrenResourcesCache.addAll((Collection) preparedList);
                    Level level = Minecraft.getInstance().level;
                    // clear existing lazy holder sets

                    if (level != null) {
                        try {
                            applyWithLevel(level.registryAccess(), false);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, gameExecutor);
    }

    public void applyWithLevel(RegistryAccess registryAccess, boolean firstLogin) {
        Minecraft mc = Minecraft.getInstance();

        Stopwatch stopwatch = Stopwatch.createStarted();
        resetWithLevel(false);


        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        // A single broken reloader must not skip the ones after it: keep going and rethrow at the end,
        // otherwise e.g. a shader failure leaves configs unparsed and every config() expression reads 0.
        RuntimeException failure = null;

        for (int i = 0; i < childrenResourcesCache.size(); i++) {
            PartialReloader<?> c = children.get(i);
            try {
                processTyped(c, childrenResourcesCache.get(i), ops, registryAccess);
            } catch (Exception e) {
                failure = recordFailure(c, e, "parse", failure);
            }
        }

        for (var c : children) {
            try {
                c.applyWithLevel(registryAccess, firstLogin);
            } catch (Exception e) {
                failure = recordFailure(c, e, "apply", failure);
            }
        }

        if (failure != null) throw failure;

        Polytone.LOGGER.info("Reloaded Polytone Resources in {} ms", stopwatch.elapsed().toMillis());

        //refresh player inventory menu as its the only one its not re made. needed for value mod. ugly
        var player = mc.player;
        if (player != null) {
            var container = player.containerMenu;
            var inv = player.inventoryMenu;
            if (inv.getClass() == InventoryMenu.class) {
                player.inventoryMenu = new InventoryMenu(player.getInventory(), inv.active, player);
                if (container == inv) {
                    player.containerMenu = player.inventoryMenu;
                }
            }

            if (mc.options.graphicsMode().get() == GraphicsStatus.FABULOUS) {
                player.sendSystemMessage(Component.translatable("message.polytone.fabulous_warning") );
            }
        }
        Level level = mc.level;
        if (level instanceof ClientLevel cl) {
            cl.clearTintCaches();
        }
    }

    private static RuntimeException recordFailure(PartialReloader<?> reloader, Exception e, String stage,
                                                  RuntimeException previous) {
        String message = reloader + " failed to " + stage + " some resources";
        Polytone.logException(e, message);
        Polytone.iMessedUp = true;
        Polytone.LOGGER.error(message);
        if (previous != null) {
            previous.addSuppressed(e);
            return previous;
        }
        return e instanceof RuntimeException re ? re : new RuntimeException(message, e);
    }

    @SuppressWarnings("all")
    private <T> void processTyped(PartialReloader<T> reloader, Object object, RegistryOps<JsonElement> ops, RegistryAccess access) {
        //yea... we cant use registry ops here theres no level yet
        reloader.parseWithLevel((T) object, ops, access);
    }


    public void resetWithLevel(boolean isLogOff) {
        TokenBucketTracker.clear();
        for (var c : children) {
            c.resetWithLevel(isLogOff);
        }
    }

    //gather models
    public void earlyProcess(ResourceManager resourceManager) {
        for (var c : children) {
            c.earlyProcess(resourceManager);
        }
        PlatStuff.doAddModels();
    }
}
