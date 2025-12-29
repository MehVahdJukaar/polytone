package net.mehvahdjukaar.polytone.mixins.neoforge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistrySnapshot.class)
public class RegistrySnapshotMixin {


    @WrapWithCondition(method = "<init>(Lnet/minecraft/core/Registry;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/MappedRegistry;register(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;"))
    private boolean polytone$skipDynamic(MappedRegistry instance, int i, ResourceKey resourceKey, Object object, RegistrationInfo registrationInfo,
                                         @Local(argsOnly = true) Registry<?> registry) {
        //removes dynamic stuff
        return !Polytone.isEntryDynamic(registry, resourceKey.location());
        //jus relevant or LAN
    }

    @WrapWithCondition(method = "lambda$new$0", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectSortedMap;put(ILjava/lang/Object;)Ljava/lang/Object;"))
    private boolean polytone$skipDynamicId(Int2ObjectSortedMap instance, int i, Object object,@Local(argsOnly = true) Registry<?> registry,
                                           @Local(argsOnly = true) Identifier resourceKey) {
        //removes dynamic stuff
        return !Polytone.isEntryDynamic(registry, resourceKey);
        //jus relevant or LAN
    }
}
