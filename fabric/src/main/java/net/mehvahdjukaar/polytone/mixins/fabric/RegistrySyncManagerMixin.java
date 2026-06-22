package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneStub;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistrySyncManager.class)
public class RegistrySyncManagerMixin {


    @WrapOperation(method = "createAndPopulateRegistryMap",
            remap = false,
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;put(Ljava/lang/Object;I)I"))
    private static int polytone$omitPolytoneDynamicEntries(Object2IntMap instance, Object o, int i,
                                                           Operation<Integer> original, @Local Registry<?> registryId,
                                                           @Local(ordinal = 1) ResourceLocation entryId) {
        if (!PolytoneStub.isEntryDynamic(registryId, entryId)) {
            return original.call(instance, o, i);
        } else {
            return 0;
        }
    }
}
