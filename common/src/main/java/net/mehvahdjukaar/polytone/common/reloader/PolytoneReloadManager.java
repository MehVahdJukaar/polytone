package net.mehvahdjukaar.polytone.common.reloader;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.SpecialModelsHandler;
import net.mehvahdjukaar.polytone.common.TokenBucketTracker;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributesHandler;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

// Needed to reload stuff in order but still off-thread when we can in prepare
public class PolytoneReloadManager implements PreparableReloadListener {

    private final List<ContentManager<?>> children;
    private final List<AssetsFiles> childrenResourcesCache = new ArrayList<>();

    public PolytoneReloadManager(ContentManager<?>... reloaders) {
        children = List.of(reloaders);
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor, PreparationBarrier preparationBarrier, Executor gameExecutor) {
        List<CompletableFuture<?>> futures = children.stream()
                .map(child -> CompletableFuture.supplyAsync(() -> child.prepare(sharedState), backgroundExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(preparedList -> {
                    childrenResourcesCache.clear();
                    childrenResourcesCache.addAll((List<AssetsFiles>) preparedList);
                    Level level = Minecraft.getInstance().level;

                    applyNormal();

                    if (level != null) {
                        RegistryAccess ra = level.registryAccess();
                        if (ra instanceof RegistryAccess.Frozen) {
                            try {
                                applyWithLevel(ra, false);
                            } catch (Exception e) {
                                Polytone.maybeThrow(
                                        new RuntimeException(e)
                                );
                            }
                        } else {
                            Polytone.LOGGER.warn("Tried to reload with a non frozen registry access. How?");
                        }
                    }
                }, gameExecutor);
    }


    //normal apply
    public void applyNormal() {
        for (int i = 0; i < childrenResourcesCache.size(); i++) {
            ContentManager<?> c = children.get(i);
            try {
                c.applyNormal(childrenResourcesCache.get(i));
            } catch (Exception e) {
                String message = c + " failed to parse some resources";
                Polytone.logException(e, message);
                Polytone.iMessedUp = true;

                Polytone.LOGGER.error(message);
                throw e;
            }
        }
    }

    public void applyWithLevel(HolderLookup.Provider registryAccess, boolean firstLogin) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        resetWithLevel(false);

        EnvironmentAttributesHandler.reset();

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        for (int i = 0; i < childrenResourcesCache.size(); i++) {
            ContentManager<?> c = children.get(i);
            try {
                c.parseWithLevel(childrenResourcesCache.get(i), ops, registryAccess);
            } catch (Throwable e) {
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

        Polytone.LOGGER.info("Reloaded Polytone AssetsFiles in {} ms", stopwatch.elapsed().toMillis());

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

        EnvironmentAttributesHandler.refresh();

    }

    public void resetWithLevel(boolean isLogOff) {
        TokenBucketTracker.clear();
        for (var c : children) {
            c.resetWithLevel(isLogOff);
        }
    }

    public void earlyProcess(SharedState sharedState) {
        for (var c : children) {
            c.earlyProcess(sharedState);
        }
        SpecialModelsHandler.finalizeAdditions();
    }
}
