package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionType.class)
public interface DimensionTypeAccessor {

    @Accessor(value = "hasSkyLight")
    void setHasSkyLight(boolean hasSkyLight);

    @Accessor(value = "skybox")
    void setSkybox(DimensionType.Skybox skybox);

    @Accessor(value = "cardinalLightType")
    void setCardinalLightType(DimensionType.CardinalLightType cardinalLightType);

    @Accessor(value = "ambientLight")
    void setAmbientLight(float ambientLight);

    @Accessor(value = "attributes")
    void setAttributes(EnvironmentAttributeMap attributes);
}
