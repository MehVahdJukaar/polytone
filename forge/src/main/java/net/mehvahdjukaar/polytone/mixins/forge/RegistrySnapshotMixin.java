package net.mehvahdjukaar.polytone.mixins.forge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import org.checkerframework.checker.units.qual.K;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ForgeRegistry.class)
public abstract class RegistrySnapshotMixin<V> {


    @Shadow public abstract ResourceKey<Registry<V>> getRegistryKey();

    @WrapWithCondition(method = "lambda$makeSnapshot$13", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;put(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Integer;"))
    private boolean polytone$skipDynamicId(Object2IntMap<ResourceLocation> instance, Object key, Integer value){
        //removes dynamic stuff
        return !Polytone.isEntryDynamic(this.getRegistryKey(),(ResourceLocation) key);
        //jus relevant or LAN
    }
}
