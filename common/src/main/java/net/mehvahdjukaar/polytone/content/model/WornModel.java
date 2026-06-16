package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Worn (equipped) model override for an item. Holds the geometry source plus the equipment layer it applies to.
 * The baked model is a {@link HumanoidModel} so NeoForge's {@code getGenericArmorModel} copies the body pose onto it;
 * this requires the geometry to declare the standard humanoid bones (head/hat, body, right_arm, left_arm,
 * right_leg, left_leg).
 */
public final class WornModel {

    private static final Codec<EquipmentClientInfo.LayerType> LAYER_TYPE_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(EquipmentClientInfo.LayerType.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown layer_type: " + s);
                }
            },
            t -> t.name().toLowerCase(Locale.ROOT));

    public static final Codec<WornModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            WornModelGeometry.CODEC.fieldOf("model").forGetter(w -> w.geometry),
            LAYER_TYPE_CODEC.optionalFieldOf("layer_type", EquipmentClientInfo.LayerType.HUMANOID).forGetter(w -> w.layerType)
    ).apply(i, WornModel::new));

    private final WornModelGeometry geometry;
    private final EquipmentClientInfo.LayerType layerType;

    // baked model cache, invalidated when the EntityModelSet instance changes (i.e. on resource reload)
    @Nullable
    private Model cachedModel;
    @Nullable
    private EntityModelSet cachedFrom;
    private boolean failed;

    public WornModel(WornModelGeometry geometry, EquipmentClientInfo.LayerType layerType) {
        this.geometry = geometry;
        this.layerType = layerType;
    }

    public boolean appliesTo(EquipmentClientInfo.LayerType type) {
        return this.layerType == type;
    }

    /** Returns the baked model for this worn override, or {@code null} if it could not be built. */
    @Nullable
    public Model getOrBake(EntityModelSet modelSet) {
        if (cachedFrom != modelSet) {
            cachedFrom = modelSet;
            cachedModel = null;
            failed = false;
        }
        if (cachedModel == null && !failed) {
            try {
                ModelPart root = geometry.bake(modelSet);
                cachedModel = new HumanoidModel<>(root);
            } catch (Exception e) {
                failed = true;
                Polytone.LOGGER.error("Failed to bake Polytone worn model", e);
            }
        }
        return cachedModel;
    }
}
