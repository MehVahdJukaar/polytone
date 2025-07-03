package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
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
                            throw new IllegalStateException(e);
                        }
                    }
                }, gameExecutor);
    }

    public void applyWithLevel(RegistryAccess registryAccess, boolean firstLogin) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (!firstLogin) resetWithLevel(false);

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        for (int i = 0; i < childrenResourcesCache.size(); i++) {
            PartialReloader<?> c = children.get(i);
            try {
                processTyped(c, childrenResourcesCache.get(i), ops, registryAccess);
            } catch (Exception e) {
                String message = c + " failed to parse some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }

        for (var c : children) {
            try {
                c.applyWithLevel(registryAccess, firstLogin);
            } catch (Exception e) {
                String message = c + " failed to apply some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }

        Polytone.LOGGER.info("Reloaded Polytone Resources in {} ms", stopwatch.elapsed().toMillis());

        //refresh player inventory menu as its the only one its not re made. needed for value mod. ugly
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var container = player.containerMenu;
            var inv = player.inventoryMenu;
            if (inv.getClass() == InventoryMenu.class) {
                player.inventoryMenu = new InventoryMenu(player.getInventory(), inv.active, player);
                if (container == inv) {
                    player.containerMenu = player.inventoryMenu;
                }
            }
        }
        Level level = Minecraft.getInstance().level;
        if (level instanceof ClientLevel cl) {
            cl.clearTintCaches();
        }
    }

    @SuppressWarnings("all")
    private <T> void processTyped(PartialReloader<T> reloader, Object object, RegistryOps<JsonElement> ops, RegistryAccess access) {
        //yea... we cant use registry ops here theres no level yet
        reloader.parseWithLevel((T) object, ops, access);
    }


    public void resetWithLevel(boolean isLogOff) {
        for (var c : children) {
            c.resetWithLevel(isLogOff);
        }
    }
    public void earlyProcess(ResourceManager resourceManager) {
        for (var c : children) {
            c.earlyProcess(resourceManager);
        }
        PlatStuff.doAddModels();
    }
}