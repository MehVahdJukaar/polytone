package net.mehvahdjukaar.polytone.mixins.forge;

import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModifiableBiomeInfo.BiomeInfo.class)
public interface ModifiableBiomeInfoBiomeInfoAccessor {

    @Mutable
    @Accessor("effects")
    void setEffects(BiomeSpecialEffects effects);
}
