package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Worn (equipped) model override for an item. Holds the geometry source plus an optional equipment slot it applies to.
 * The baked model is a {@link HumanoidModel} so it can be posed by the vanilla armor layer / NeoForge's
 * {@code getGenericArmorModel}; this requires the geometry to declare the standard humanoid bones
 * (head/hat, body, right_arm, left_arm, right_leg, left_leg).
 * <p>
 * 1.21.1 has no {@code EquipmentClientInfo.LayerType}, so the worn layer is keyed by {@link EquipmentSlot}
 * (an absent {@code slot} means it applies to every armor slot the item renders in).
 */
public final class WornModel {

    public static final Codec<WornModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            WornModelGeometry.CODEC.fieldOf("model").forGetter(w -> w.geometry),
            EquipmentSlot.CODEC.optionalFieldOf("slot").forGetter(w -> w.slot)
    ).apply(i, WornModel::new));

    private final WornModelGeometry geometry;
    private final Optional<EquipmentSlot> slot;

    // baked model cache, invalidated when the EntityModelSet instance changes (i.e. on resource reload)
    @Nullable
    private HumanoidModel<?> cachedModel;
    @Nullable
    private EntityModelSet cachedFrom;
    private boolean failed;

    public WornModel(WornModelGeometry geometry, Optional<EquipmentSlot> slot) {
        this.geometry = geometry;
        this.slot = slot;
    }

    public boolean appliesTo(EquipmentSlot slot) {
        return this.slot.isEmpty() || this.slot.get() == slot;
    }

    /** Returns the baked model for this worn override, or {@code null} if it could not be built. */
    @Nullable
    public HumanoidModel<?> getOrBake(EntityModelSet modelSet) {
        if (cachedFrom != modelSet) {
            cachedFrom = modelSet;
            cachedModel = null;
            failed = false;
        }
        if (cachedModel == null && !failed) {
            try {
                ModelPart root = geometry.bake(modelSet);
                cachedModel = new HumanoidModel<LivingEntity>(root);
            } catch (Exception e) {
                failed = true;
                Polytone.LOGGER.error("Failed to bake Polytone worn model", e);
            }
        }
        return cachedModel;
    }
}
