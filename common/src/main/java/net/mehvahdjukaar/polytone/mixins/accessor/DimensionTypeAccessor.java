package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionType.class)
public interface DimensionTypeAccessor {

    @Mutable
    @Accessor(value = "hasSkyLight")
    void setHasSkyLight(boolean hasSkyLight);

    @Mutable
    @Accessor(value = "skybox")
    void setSkybox(DimensionType.Skybox skybox);

    @Mutable
    @Accessor(value = "cardinalLightType")
    void setCardinalLightType(CardinalLighting.Type cardinalLightType);

    @Mutable
    @Accessor(value = "ambientLight")
    void setAmbientLight(float ambientLight);

    @Mutable
    @Accessor(value = "attributes")
    void setAttributes(EnvironmentAttributeMap attributes);
}
