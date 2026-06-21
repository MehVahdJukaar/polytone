package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A worn model source: either a reference to a registered {@link CustomModelsManager} layer (baked through the
 * game's {@code EntityModelSet}, so model replacing mods like EMF can hook it) or an inlined {@link ModelDefinition}
 * (baked directly). Used inside item modifiers via the usual reference-or-direct pattern.
 */
public final class WornModelGeometry {

    @Nullable
    private final ResourceLocation reference;
    @Nullable
    private final ModelDefinition inline;
    @Nullable
    private LayerDefinition inlineLayer;

    private WornModelGeometry(@Nullable ResourceLocation reference, @Nullable ModelDefinition inline) {
        this.reference = reference;
        this.inline = inline;
    }

    public static final Codec<WornModelGeometry> CODEC = CodecUtils.referenceOrDirect(
            ResourceLocation.CODEC.xmap(id -> new WornModelGeometry(id, null), g -> g.reference),
            ModelDefinition.CODEC.xmap(d -> new WornModelGeometry(null, d), g -> g.inline)
    );

    public ModelPart bake(EntityModelSet modelSet) {
        if (reference != null) {
            return modelSet.bakeLayer(CustomModelsManager.layerLocation(reference));
        }
        if (inlineLayer == null) {
            inlineLayer = inline.toLayerDefinition();
        }
        return inlineLayer.bakeRoot();
    }
}
