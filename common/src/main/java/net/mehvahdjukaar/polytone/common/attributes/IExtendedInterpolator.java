package net.mehvahdjukaar.polytone.common.attributes;

import net.minecraft.world.attribute.SpatialAttributeInterpolator;

public interface IExtendedInterpolator {

    SpatialAttributeInterpolator polytone$getOrCreatePostInterpolator();
}
