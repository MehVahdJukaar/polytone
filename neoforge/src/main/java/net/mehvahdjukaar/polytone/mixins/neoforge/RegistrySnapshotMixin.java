package net.mehvahdjukaar.polytone.mixins.neoforge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneStub;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistrySnapshot.class)
public class RegistrySnapshotMixin {


    @WrapOperation(method = "<init>(Lnet/minecraft/core/Registry;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/MappedRegistry;register(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;"))
    private Holder.Reference polytone$skipDynamic(MappedRegistry instance, int i, ResourceKey resourceKey, Object object, RegistrationInfo registrationInfo,
                                                  Operation<Holder.Reference> original, @Local(argsOnly = true) Registry<?> registry) {
        //removes dynamic stuff
        if (!PolytoneStub.isEntryDynamic(registry, resourceKey.location())) {
            return original.call(instance, i, resourceKey, object, registrationInfo);
        } else {
            return null;
        }
        //jus relevant or LAN
    }

    @WrapOperation(method = "lambda$new$0", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectSortedMap;put(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object polytone$skipDynamicId(Int2ObjectSortedMap instance, int i, Object object, Operation<Object> original, @Local(argsOnly = true) Registry<?> registry,
                                          @Local(argsOnly = true) ResourceLocation resourceKey) {
        //removes dynamic stuff
        if (!PolytoneStub.isEntryDynamic(registry, resourceKey)) {
            return original.call(instance, i, object);
        } else {
            return null;
        }
        //jus relevant or LAN
    }
}
