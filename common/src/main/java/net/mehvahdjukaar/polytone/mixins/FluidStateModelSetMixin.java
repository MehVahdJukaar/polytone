package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Replaces a fluid's render tint (FluidModel#tintSource) with a Polytone colormap when one targets
// that fluid. This is the vanilla 26.2 fluid coloring hook - IColorGetter extends BlockTintSource, so
// it plugs straight in. Being a vanilla class it covers both loaders and Sodium (which reads the same
// FluidModel#tintSource for non-water fluids) in one place, replacing the removed NeoForge getTintColor path.
@Mixin(FluidStateModelSet.class)
public class FluidStateModelSetMixin {

    // Cache of tinted model copies only (never the untinted original), keyed by fluid. Concurrent
    // because get() runs on chunk-build worker threads. A new FluidStateModelSet - and thus a fresh
    // cache - is built on every resource reload, in lockstep with the Polytone tints, so entries can't
    // go stale. We deliberately don't cache "untinted": that keeps a fluid meshed before Polytone's
    // apply (worker vs. game-thread races on login) from being pinned untinted forever.
    @Unique
    private final Map<Fluid, FluidModel> polytone$tintedModels = new ConcurrentHashMap<>();

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidModel polytone$applyColormap(FluidModel original, FluidState state) {
        Fluid fluid = state.getType();
        FluidModel cached = polytone$tintedModels.get(fluid);
        if (cached != null) return cached;

        IColorGetter tint = Polytone.FLUID_MODIFIERS.getConcurrentTint(fluid);
        if (tint == null) return original;

        FluidModel tinted = new FluidModel(original.layer(), original.stillMaterial(),
                original.flowingMaterial(), original.overlayMaterial(), tint);
        polytone$tintedModels.put(fluid, tinted);
        return tinted;
    }
}
