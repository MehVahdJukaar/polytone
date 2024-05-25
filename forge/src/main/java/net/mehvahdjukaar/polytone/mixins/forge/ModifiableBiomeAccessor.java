package net.mehvahdjukaar.polytone.mixins.forge;

import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModifiableBiomeInfo.class)
public interface ModifiableBiomeAccessor {

    @Accessor("modifiedBiomeInfo")
    void setModifiedBiomeInfo(ModifiableBiomeInfo.BiomeInfo info);
}
