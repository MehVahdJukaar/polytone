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

public final class WornModel {

    public static final Codec<WornModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            WornModelGeometry.CODEC.fieldOf("model").forGetter(w -> w.geometry),
            EquipmentSlot.CODEC.optionalFieldOf("slot").forGetter(w -> w.slot)
    ).apply(i, WornModel::new));

    private final WornModelGeometry geometry;
    private final Optional<EquipmentSlot> slot;

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
                cachedModel = new HumanoidModel<>(root);
            } catch (Exception e) {
                failed = true;
                Polytone.LOGGER.error("Failed to bake Polytone worn model", e);
            }
        }
        return cachedModel;
    }
}
