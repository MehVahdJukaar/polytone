package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.flashback.RegistryMetaHelper;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashSet;

@Pseudo
@Mixin(RegistryMetaHelper.class)
public class FlashbackRegistryMetaHelperMixin {

    @WrapWithCondition(method = "calculateNamespacesForRegistries", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false, target = "Ljava/util/LinkedHashSet;add(Ljava/lang/Object;)Z"))
    private static boolean polytone$skipDynamicEntries(LinkedHashSet<?> instance, Object namespace,
                                                       @Local Registry<?> registry, @Local(ordinal = 1) Identifier location) {
        return !Polytone.isEntryDynamic(registry, location);
    }
}
