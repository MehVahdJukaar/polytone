package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class EntityModelGeometry {

    @Nullable
    private final ResourceLocation reference;
    @Nullable
    private final ModelDefinition inline;
    @Nullable
    private LayerDefinition inlineLayer;

    private EntityModelGeometry(@Nullable ResourceLocation reference, @Nullable ModelDefinition inline) {
        this.reference = reference;
        this.inline = inline;
    }

    public static final Codec<EntityModelGeometry> CODEC = SchemaCodecs.referenceOrDirect(
            ResourceLocation.CODEC.xmap(id -> new EntityModelGeometry(id, null), g -> g.reference),
            ModelDefinition.CODEC.xmap(d -> new EntityModelGeometry(null, d), g -> g.inline));

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
