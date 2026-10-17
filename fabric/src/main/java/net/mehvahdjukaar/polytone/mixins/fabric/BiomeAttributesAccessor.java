package net.mehvahdjukaar.polytone.mixins.fabric;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.class)
public interface BiomeAttributesAccessor {

    @Mutable
    @Accessor("attributes")
    void setAttributes(EnvironmentAttributeMap attributes);
}
